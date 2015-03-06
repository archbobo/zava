package com.zqh.java.dmap;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.zqh.java.dmap.util.CompressionUtils;
import com.zqh.java.dmap.util.ExtendedFileChannel;
import org.iq80.snappy.Snappy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zqh.java.dmap.util.ByteArray;

/**
 * Builder for the DMap. The DMapBuilder is a write-once builder, as DMap is
 * read-only.
 * 
 * Improve:
 *  - Spill keys to disk every K bytes.
 *  - Make appendable.
 *  - Make iterable.
 *  - Compress using varint or delta-encoding
 */
public class DMapBuilder {

  /** Default Key-Value Block size (in bytes) - set to 1 MB. */
  private static final int DEFAULT_BLOCK_SIZE = 1048576;

  /** Current block size for the file*/
  private int blockSize_;
  
  /** Compress values */
  private boolean compressValues_;

  /** Map file to write to. */
  private File mapFile_;

  /** Writer to the map file. */
  private ExtendedFileChannel output_;

  /** Temporary Map file. */
  private File tmpMapFile_;

  /** Writer to the temp file. */
  private ExtendedFileChannel tmpOutput_;

  /** Keep track of number of entries retrieved from original file */
  private int entriesCount_;

  private final Logger logger_ = LoggerFactory.getLogger(DMapBuilder.class);

  public DMapBuilder(File mapFile) throws IOException {
    this(mapFile, DEFAULT_BLOCK_SIZE);
  }
  
  public DMapBuilder(File mapFile, int blockSize) throws IOException {
    this(mapFile, blockSize, true);
  }

  /**
   * DMap的文件实例. 将Map数据以磁盘Disk形式存储. 所以叫做DMap=DiskMap
   * @param mapFile Map File instance.
   * @param blockSize Size of a block (in bytes).
   * @throws IOException
   */
  public DMapBuilder(File mapFile, int blockSize, boolean compressValues) throws IOException {
    boolean success = mapFile.createNewFile();
    if (success) {
      blockSize_ = blockSize;
      compressValues_ = compressValues;
      mapFile_ = mapFile;
      try {
        //创建临时文件
        tmpMapFile_ = File.createTempFile("tmpDMap_", "_" + mapFile.getName());
      } catch (Exception e) {
        throw new IOException("Error creating intermediate file: " + tmpMapFile_ + ", cannot write.");
      }
      //临时文件输出流
      tmpOutput_ = new ExtendedFileChannel(new RandomAccessFile(tmpMapFile_, "rw").getChannel());
      
      //DMap的输出流, DMap用的文件是mapFile.
      output_ = new ExtendedFileChannel(new RandomAccessFile(mapFile_, "rw").getChannel());
    } else {
      throw new IOException("Output map file already exists at: " + mapFile
          + ", cannot write.");
    }
  }

  //往Map中添加一对<Key,Value>
  public void add(byte[] key, byte[] value) throws IOException {
    //首先写到临时文件输出流, 即写到临时文件中
    //分别是Key的长度,Value的长度,然后才是Key,Value
    tmpOutput_.writeInt(key.length);
    tmpOutput_.writeInt(value.length);
    tmpOutput_.write(key);
    tmpOutput_.write(value);
    //Map的条目数量+1
    entriesCount_++;
  }

  //构建MapFile, 添加Map数据时,数据只是追加到临时文件中.
  //build才是正式写到文件中,不能简单地追加数据到文件中.要考虑到get操作的高效读取.
  public void build() throws IOException {
    Map<ByteArray, Long> tmpKeyOffsetMap =  new HashMap<>();
    //要先flush,如果没有flush,则数据还是在内存中:输出流对象中.flush之后,才写到临时文件中
    tmpOutput_.flush();
    tmpOutput_.close();

    //读取临时文件
    ExtendedFileChannel raf = new ExtendedFileChannel(new RandomAccessFile(tmpMapFile_, "r").getChannel());
    //key和value是之前add到输出流中的每对数据
    byte[] key;
    byte[] value;
    long currentOffset = 0;

    try {
      logger_.debug("Keys to process: " + entriesCount_);
      for(int i=0;i<entriesCount_;i++) {
        //首先读取key的长度和value的长度,这也是<Key,Value>数据通过add写到临时文件中的顺序
        int keyLen = raf.readInt();
        int valLen = raf.readInt();

        //读取key内容
        key = new byte[keyLen];
        raf.read(key);
        //key不能重复!
        if(tmpKeyOffsetMap.containsKey(new ByteArray(key))) {
          throw new IOException("Duplicate key encountered: " + key);
        }

        //key在临时文件中的offset.初始值为0,因为第一个key在文件的第0个位置
        tmpKeyOffsetMap.put(new ByteArray(key), currentOffset);
        // ignore the value byte sequence (for the time being) and move to next record start pos
        //忽略value的值.先把所有的key->offset的对应关系放到map中

        //定位到下一个key的offset.  firstKeyLen|firstValLen|firestKey|firstVal|secKeyLen
        //                           keyLen      valLen      key      value |<-currentOffset
        currentOffset = raf.position() + valLen;
        raf.position(currentOffset);
      }

      logger_.debug("Loaded " + tmpKeyOffsetMap.size() + " keys from temporary file");

      // global header - version, entries count, block size, trailer offset
      output_.writeInt(DMap.VERSION);
      output_.writeInt(tmpKeyOffsetMap.size());
      output_.writeInt(blockSize_);
      output_.writeBool(compressValues_);
      // insert placeholder for trailer offset
      output_.writeLong(0);

      //所有的key,对key进行排序. 排序的目的是为了查找时快速定位key的位置
      List<ByteArray> allKeys = new ArrayList<>(tmpKeyOffsetMap.keySet());
      Collections.sort(allKeys);
      logger_.info("Writing map for " + allKeys.size() + " keys.");

      long globalOffset = output_.position();
      int currentBlockOffset = 0;
      int remainingBytes = blockSize_;
      ByteArray firstKey = null;

      //Block级别的key->offset, 一个文件会有多个Block!
      //File = Block1 | Block2 | Block3 | ...
      // Map to store block-level key-offset pairs (to be written to each block trailer)
      Map<ByteArray, Integer> blockKeyOffset_ = new HashMap<>();
      //数据块的起始和结束位置(全局)
      // Map to store blockStart-blockTrailerStart pair (to be written to global trailer)
      Map<Long, Long> blockTrailerOffsets = new HashMap<>();
      //每个Block的第一个key:<CurrentBlockOffset, theFirstKeyOfThisBlock>
      // Map to store blockStart-firstKey pair (to be written to global trailer)
      Map<Long, ByteArray> blockFirstKey = new HashMap<>();

      //开始循环处理每对Key,Value
      for (ByteArray keyBytes : allKeys) {
        long offset = tmpKeyOffsetMap.get(keyBytes);
        raf.position(offset);
        int keyLen = raf.readInt();
        int valLen = raf.readInt();
        value = new byte[valLen];
        // position pointer at the starting of value data
        raf.position(raf.position() + keyLen);
        raf.read(value);

        if (compressValues_) {
          value = Snappy.compress(value);
        }

        //数据的长度,即每一个<Key,Value>键值对的值的长度
        int dataLength = CompressionUtils.getVNumSize(value.length) + value.length;
        //一个KeyValue的value长度比BlockSize还大?
        if(dataLength > blockSize_) {
          throw new IOException("Data size ("+ dataLength +" bytes) greater than specified block size(" + blockSize_ + " bytes)");
        }

        // write block trailer & reset variables
        //dataLength是每一个KeyValue, remainingBytes的初始值是BlockSize,在处理完一个KV后值会减少这个KV的长度
        //如果当前写入的KV比剩余空间大,说明剩余的空间不够当前KV的写入, 则要重新创建一个文件,并重置变量
        if(dataLength > remainingBytes) {
          logger_.debug("Key : " + keyBytes + " with value doesnt fit in remaining "+ remainingBytes + " bytes.");
          globalOffset = updateBlockTrailer(blockKeyOffset_, blockTrailerOffsets, blockFirstKey, firstKey, globalOffset);
          logger_.debug("Creating new block @ " + globalOffset);
          currentBlockOffset = 0;
          remainingBytes = blockSize_;
          firstKey = null;
        }
        //每个Block的firstKey. 在新建Block的时候,重置了firstKey=null,这里会再次执行. 只有在每个Block的开始才会执行!
        if(firstKey == null) firstKey = keyBytes;

        //写入Value. 还没有写入key?? 什么时候写入key? 调用updateBlockTrailer的时候. 如果数据没有满一个Block,则不会调用
        logger_.debug("write@ " + globalOffset + " key: " + keyBytes + ""
          + " (hash: " + keyBytes.hashCode() + ")");
        output_.writeVInt(value.length);
        // write value (key can be retrieved from block trailer)
        output_.write(value);
        // store key-offset pair (needed for block trailer)
        //在每一个Block里的key->offset的映射[offset针对的是当前Block]. 之前有一个tmpKeyOffsetMap是key在整个临时文件中的offset
        //注意:blockKeyOffset的value是Map的key对应的value在Block中的offset. 通过key得到value在Block中的位置即可读取出value
        blockKeyOffset_.put(keyBytes, currentBlockOffset);
        //Block的大小是Value的长度总和,不包括Key,blockOffset增加,则remainingBytes减少
        currentBlockOffset += dataLength;
        remainingBytes -= dataLength;
      }

      // write the last block trailer information
      globalOffset = updateBlockTrailer(blockKeyOffset_, blockTrailerOffsets, blockFirstKey, firstKey, globalOffset);

      //准备写入GlobalBlockTrailer信息.此时的globalOffset就是BlockTrailer的offset
      //总的文件格式如下:
      // Header | Block1 | Block1's Trailer | Block2 | Block2's Trailer | ... | GlobalBlockTrailer
      // 对于每个Block+BlockTrailer的格式如下:
      //|Block1                        |Block1's Trailer
      // val1Len,val1,val2Len,val2,....,numKeys,key1Len,key1,val1'offset,key2Len,key2,val2'offset
      // GlobalTrailer的格式如下:
      // numBlocks,block1'off,block1's trailer'off,key1Len,key1

      // write global trailer (block start offset-block trailer offset pair & first key in the block)
      //有多少个数据块
      output_.writeVInt(blockTrailerOffsets.size());
      //blockTrailerOffsets放的是BlockStartOffset->BlockTrailerOffset,即每个数据块的全局开始位置和结束位置
      List<Long> allBlockKeys = new ArrayList<>(blockTrailerOffsets.keySet());
      Collections.sort(allBlockKeys);
      for(long blockStart : allBlockKeys) {
        //写入每个Block在文件中的开始位置和结束位置
        output_.writeVLong(blockStart);
        output_.writeVLong(blockTrailerOffsets.get(blockStart));
        //blockFirstKey放的是BlockStartOffset和这个Block的第一个Key
        byte[] tmpFirstKeyByte = blockFirstKey.get(blockStart).getBytes();
        // write the first key info to global trailer
        output_.writeVInt(tmpFirstKeyByte.length);
        output_.write(tmpFirstKeyByte);
      }
      raf.close();
      output_.flush();
      output_.close();

      // fill in the previously created placeholder for trailer offset
      raf = new ExtendedFileChannel(new RandomAccessFile(mapFile_, "rw").getChannel());
      raf.position(DMap.DEFAULT_LOC_FOR_TRAILER_OFFSET);
      logger_.info("DMap Trailer start at " + globalOffset + ".");
      //在指定位置填充BlockTrailer的Offset, 这个位置首先写入了BlockCount.见前面!
      raf.writeLong(globalOffset);
    } finally {
      // delete the intermediate temp file
      tmpMapFile_.delete();
      raf.close();
    }
  }

    /**
     * Returns new global offset after updating the block trailer
     * @param keyOffsets key-->key对应的value在当前Block的offset
     * @param blockTrailerOffsets
     * @param blockFirstKey 当前Block的Offset(Block在文件的全局offset)-->当前Block的第一个Key
     * @param firstKey 当前Block的firstKey
     * @param globalOffset 当前Block在文件中的全局位置
     * @return
     * @throws IOException
     */
  private long updateBlockTrailer(Map<ByteArray, Integer> keyOffsets,
      Map<Long, Long> blockTrailerOffsets,
      Map<Long, ByteArray> blockFirstKey,
      ByteArray firstKey, long globalOffset) throws IOException {
    //文件当前的位置,即当前Block的末尾.注意Block不包括Key,只包含Value. Block的末尾是准备写key的开始. Block可以理解为数据块
    //|value1|value2|....|key1|v1-off|key2|v2-off... |val10|val11|....|key10|val10-off|.|...|
    //|<-----Block A---->|<----Block A Trailer------>|<--Block B----->|<--Block B Trailer-->|
    //blockTrailerOffset: value1'off->key1'off, value10'off->key10'off
    //blockFirstKey: value1'off->key1, value10'off->key10
    long trailerOffset = output_.size();

    // write number of entries in the current block 当前Block的key数量
    output_.writeVInt(keyOffsets.size());
    for(Entry<ByteArray, Integer> e : keyOffsets.entrySet()) {
      ByteArray byteArray = e.getKey();
      //写入keyLen, Key,
      output_.writeVInt(byteArray.getBytes().length);
      output_.write(byteArray.getBytes());
      //key对应的value在当前Block的offset
      output_.writeVInt(e.getValue());
    }
    keyOffsets.clear();
    // track block offset info
    blockTrailerOffsets.put(globalOffset, trailerOffset);
    // track first keys in each block 每个Block的firstKey
    blockFirstKey.put(globalOffset, firstKey);
    return output_.position();
  }
}