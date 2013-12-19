#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "mydisk.h"

/* The cache entry struct */
struct cache_entry
{
	int block_id;
	int is_dirty;
	char content[BLOCK_SIZE];
};

int cache_blocks;  /* number of blocks for the cache buffer */
int cache_struct_size = sizeof(struct cache_entry);
int insert_pos=0;
int cache_full=0;

struct cache_entry *cache_start = NULL;

int init_cache(int nblocks)
{
	cache_enabled = 1;
	cache_blocks = nblocks;
	struct cache_entry *cache = malloc(nblocks*sizeof(struct cache_entry));
	int a;
	for (a=0; a < nblocks; a++) {
		cache[a].is_dirty=0;
		cache[a].block_id = -1; //to mean that it is empty
		memset(cache[a].content, 0, BLOCK_SIZE);
	}
	cache_start = cache;
//	////printf("cache  block id1]: %d struct size: %d\n", first[2].block_id, cache_struct_size);
	return 0;
}

int close_cache()
{
	if (cache_enabled) {
		int c;
		for (c=0; c<cache_blocks; c++) {
			if (cache_start[c].is_dirty) {
				////printf("close_cache: writing dirty block with id:%d\n", cache_start[c].block_id);
				cache_enabled=0;
				mydisk_write_block(cache_start[c].block_id, &(cache_start[c].content));
			}	
		}
		cache_full=0;
		free(cache_start);
		return 0;
	}
}

void create_write_block(int block_id, void *buffer) {
	//printf("creating cached write block with id: %d\n", block_id);
	int c, pos_to_write = -1;
	for (c = 0; c < cache_blocks; c++) {//check if there is an existing block and overwrite it to avoid problems
		if (cache_start[c].block_id == block_id) {
			//printf("in create_write, found blockid:%d\n", block_id);
			pos_to_write = c;
		}
	}
	if (pos_to_write == -1) {//need to find spot where to write the cache block
		pos_to_write = insert_pos;
		if (cache_full) {//we need to write the block to disk if it's dirty before overwriting it
			////printf("add_write: cache full\n");
			if (cache_start[pos_to_write].is_dirty) {
				cache_enabled=0;
				mydisk_write_block(cache_start[pos_to_write].block_id, &(cache_start[pos_to_write].content));
				cache_enabled=1;
				//printf("create_write: writing dirty block with id:%d\n", block_id);
			}
		}
	}

	//write the block content into the cache block
	cache_start[pos_to_write].block_id = block_id;
	memcpy(&(cache_start[pos_to_write].content), buffer, BLOCK_SIZE);
	//printf("setting dirty block\n");
	cache_start[pos_to_write].is_dirty = 1;

	if ((insert_pos+1) >= cache_blocks) {
		////printf("cache full (ignore if more than once)\n");
		cache_full = 1;
	}
	insert_pos = (insert_pos+1)%cache_blocks;//update insert position
	return NULL;

}

void *get_cached_block(int block_id)
{
	int c;
	for (c = 0; c < cache_blocks; c++) {
		////printf("looked at blockid:%d\n", cache_start[c].block_id);
		if (cache_start[c].block_id == block_id) {
			//printf("found blockid:%d\n", block_id);
			return &(cache_start[c].content);
		}
	}
	//printf("couldnt find block_id:%d\n", block_id);
	return NULL;
}

void *get_dirty_cached_block(int block_id, void* buffer) {//used by mydisk_block_write to get the most recent write block
	int c;
	for (c = 0; c < cache_blocks; c++) {
		////printf("looked at blockid:%d\n", cache_start[c].block_id);
		if ((cache_start[c].block_id == block_id)&&(cache_start[c].is_dirty)&&(!memcmp(&(cache_start[c].content), buffer, BLOCK_SIZE))) {
			//printf("found dirty blockid:%dwith string:%s\n", block_id, cache_start[c].content);
			return &(cache_start[c].content);
		}
	}
	//printf("couldnt find dirty block_id:%d\n", block_id);
	return NULL;
}

void *create_cached_block(int block_id)
{
	//printf("creating cached block with id: %d\n", block_id);
	char *temp = (char *)malloc(BLOCK_SIZE);
	//write the block content into the cache block
	cache_enabled = 0; //turn off caching temporarily to avoid infinite loop
	mydisk_read_block(block_id, temp);
	cache_enabled = 1;
	
	if (cache_full) {//we need to write the block to disk if it's dirty before overwriting it
		////printf("cache full\n");
		if (cache_start[insert_pos].is_dirty) {
			cache_enabled=0;
			mydisk_write_block(cache_start[insert_pos].block_id, &(cache_start[insert_pos].content));
			cache_enabled=1;
			//printf("writing dirty block with id:%d\n", block_id);
		}
	}

	cache_start[insert_pos].block_id = block_id;
	memcpy(&(cache_start[insert_pos].content), temp, BLOCK_SIZE);
	cache_start[insert_pos].is_dirty = 0;
	free(temp);
	if ((insert_pos+1) >= cache_blocks) {
		//printf("cache full now (ignore if more than once)\n");
		cache_full = 1;
	}
	insert_pos = (insert_pos+1)%cache_blocks;//update insert position
	return NULL;
}

void mark_dirty(int block_id)
{
	int c;
	for (c = 0; c < cache_blocks; c++) {
		if (cache_start[c].block_id == block_id) {
			////printf("marking blockid:%d as dirty\n", block_id);
			cache_start[c].is_dirty = 1;
		}
	}
	return;
}

