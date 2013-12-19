#ifndef MYDISK_H_
#define MYDISK_H_

#include <stdio.h>

#define BLOCK_SIZE 512

#define HDD_SEEK 10
#define HDD_READ_LATENCY 2
#define HDD_WRITE_LATENCY 2
#define SSD_READ_LATENCY 1
#define SSD_WRITE_LATENCY 1
#define MEMORY_LATENCY 1

extern FILE *thefile;     /* the file that stores all blocks */
extern int max_blocks;    /* max number of blocks given at initialization */
extern int disk_type;     /* disk type, 0 for HDD and 1 for SSD */
extern int cache_enabled; /* is cache enabled? 0 no, 1 yes */

/**
 * initialize the disk
 * file_name: the name of the file which you store all the blocks
 * nblocks: the maximum number of blocks 
 * type: 0 for HDD, 1 for SSD
 * RETURN 0 if success, 1 if any error
 */
int mydisk_init(char const *file_name, int nblocks, int type);

/**
 * read a single block
 * RETURN 0 if success, 1 if block id is invalid
 * When caching is enabled, return 0 if cache miss, 
 * 1 if parameters are invalid, -1 if cache hit.
 */
int mydisk_read_block(int block_id, void *buffer);

/**
 * write a single block
 * RETURN 0 if success, 1 if block id is invalid
 * When caching is enabled, return 0 if cache miss, 
 * 1 if parameters are invalid, -1 if cache hit.
 */
int mydisk_write_block(int block_id, void *buffer);

/**
 * read bytes from the flat disk
 * start_address: start address in the flat space
 * nbytes: number of bytes to be read
 * buffer: the destination memory buffer
 * RETURN 0 if success, 1 if the address is invalid
 */
int mydisk_read(int start_address, int nbytes, void *buffer);

/**
 * write bytes to the flat disk
 * start_address: start address in the flat space
 * nbytes: number of bytes to be written
 * buffer: the source memory buffer
 * RETURN 0 if success, 1 if the address is invalid
 */
int mydisk_write(int start_address, int size, void *buffer);

/**
 * close the disk
 */
void mydisk_close();

/********************************** CACHING PART ********************/
/**
 * initialize the cache with nblocks blocks
 * RETURN 0 if success, 1 if any error
 */
int init_cache(int nblocks);

/**
 * cleanup the cache
 * RETURN 0 if success, 1 if any error
 */
int close_cache();

/**
 * get a cached block. If not present in the cache, return NULL.
 */
void *get_cached_block(int block_id);

/**
 * put the block_id in the cache. There are two possibilities:
 * 1. If not present, create a new block and return it.
 * 2. If present, return the block.
 * Note that creatinig a new block may kick some existing one. 
 *
 */
void *create_cached_block(int block_id);

/**
 * mark the block in the cache as dirty. If not present, do nothing.
 */
void mark_dirty(int block_id);

/********************************** LATENCY PART ********************/
/**
 * report the latecny, which will be displayed
 */
void report_latency(int latency);

#endif
