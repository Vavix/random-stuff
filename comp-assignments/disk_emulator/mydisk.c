#include "mydisk.h"
#include <string.h>

FILE *thefile;     /* the file that stores all blocks */
int max_blocks;    /* max number of blocks given at initialization */
int disk_type;     /* disk type, 0 for HDD and 1 for SSD */
int cache_enabled = 0; /* is cache enabled? 0 no, 1 yes */

int mydisk_init(char const *file_name, int nblocks, int type)
{
	disk_type = type;
	max_blocks = nblocks;
	thefile = fopen(file_name, "w+b");

	if(thefile == NULL) return 1;

	char *array = (char *)malloc(BLOCK_SIZE * nblocks);
	memset(array, '\0', BLOCK_SIZE*nblocks);
	
	if (fwrite(array, BLOCK_SIZE, nblocks, thefile) != nblocks) {
		//printf("error writing to file\n"); 
		free(array);
		return 1;
	}
	free(array);
	return 0;

}

void mydisk_close()
{
	fclose(thefile);
}

int mydisk_read_block(int block_id, void *buffer)
{
	//printf("readblock\n");
	if ((block_id >= max_blocks)||(block_id < 0)) {
		//printf("bad arguments in mydisk_read_block\n");
		return 1;
	}
	if (cache_enabled) {
		char *temp = (char *)malloc(BLOCK_SIZE);
		int return_val = -1;
		temp = get_cached_block(block_id);

		if (temp==NULL) {//cache miss, put the block into the cache
			//printf("cache miss in readblock\n");
			create_cached_block(block_id);
			temp = get_cached_block(block_id);//get the block again
			return_val = 0;
		}
		memcpy(buffer, temp, BLOCK_SIZE);

		return return_val;
	} else {
		char *array = (char *)malloc(BLOCK_SIZE);
		//printf("fseek at: %d\n", BLOCK_SIZE*block_id);
		////printf("thefile:%x\n", thefile);
		fseek(thefile, BLOCK_SIZE*block_id, SEEK_SET);
		if (fread(array, BLOCK_SIZE, 1, thefile)!=1) {
			//printf("error in fread in mydisk_read_block while trying to read block_id: %d", block_id);
			return 1;
		}
		memcpy(buffer, array, BLOCK_SIZE);
		free(array);
		return 0;
	}
}

int mydisk_write_block(int block_id, void *buffer)
{
	//printf("writeblock\n");
	if ((block_id < 0)||(block_id >= max_blocks)) return 1;
	
	if (cache_enabled) {
		int return_val = -1;
		char *temp = (char *)malloc(BLOCK_SIZE);
		temp = get_dirty_cached_block(block_id, buffer);

		if (temp==NULL) {
			//printf("creating new write block\n");
			create_write_block(block_id, buffer);
			//printf("getting block again\n");
			temp = get_cached_block(block_id);
			return_val = 0;
		}
		return return_val;
	} else {
		char *array = (char *)malloc(BLOCK_SIZE);
		memcpy(array,buffer,BLOCK_SIZE);
		//printf("writing to file:%s\n", array);
		fseek(thefile, BLOCK_SIZE*block_id, SEEK_SET);
		if (fwrite(array, BLOCK_SIZE, 1, thefile)!=1) {
			free(array);
			return 1;
		}
		free(array);
		return 0;
	}
}

int mydisk_read(int start_address, int nbytes, void *buffer)
{
	int offset, remaining, amount, block_id, written, latency=0;
	int cache_hit = 0, cache_miss = 0;
	if 	((start_address > max_blocks*BLOCK_SIZE)||
		(nbytes < 0)||(start_address < 0)||
		((start_address+nbytes) > max_blocks*BLOCK_SIZE))
	{
		//printf("bad arguments in mydisk_read\n");
		return 1;
	}
	if (nbytes==0) return 0; //nothing to do here, move along

	offset = start_address%BLOCK_SIZE;
	remaining = (start_address+nbytes)%BLOCK_SIZE;
	amount = (BLOCK_SIZE - offset)%BLOCK_SIZE;
	//get second half of first block, if needed

	if ((offset!=0)||(start_address/BLOCK_SIZE == (start_address+nbytes)/BLOCK_SIZE)) {
		if (disk_type==0) latency += HDD_SEEK+HDD_READ_LATENCY;
		else latency += SSD_READ_LATENCY;
		char *first_block = (char *)malloc(BLOCK_SIZE);
		mydisk_read_block(start_address/BLOCK_SIZE, first_block);
		//printf("firstblock: %s\n", first_block);
		if (start_address/BLOCK_SIZE == (start_address+nbytes)/BLOCK_SIZE) {
			memcpy(buffer, first_block+offset, nbytes);
		} else {
			memcpy(buffer, first_block+offset, amount);
		}
		free(first_block);
	}
	//printf("off: %d rem: %d 1st block at: %d last block at: %d\n", offset, remaining, start_address/BLOCK_SIZE, (start_address+nbytes)/BLOCK_SIZE);
	//get the middle blocks
	int loops, loop_index;
	if (offset!=0) {
		loops = (nbytes-amount)/BLOCK_SIZE;
		loop_index = start_address/BLOCK_SIZE+1;
	}
	else {
		loops = nbytes/BLOCK_SIZE-1;
		loop_index = start_address/BLOCK_SIZE;
	}

	char *temp = (char *)malloc(BLOCK_SIZE);
	if (start_address/BLOCK_SIZE < (start_address+nbytes)/BLOCK_SIZE) {
		int i, c;
		if (disk_type==0) latency += HDD_SEEK;
		for (i=0,c=loop_index; i<=loops; c++, i++) {
			memset(temp, 0, BLOCK_SIZE);
			//printf("read loop#%d \n", c);
			mydisk_read_block(c, temp);
			//printf("reading temp: %s\n", temp);
			memcpy(buffer+amount+i*BLOCK_SIZE, temp, BLOCK_SIZE);
			if (disk_type==0) latency += HDD_READ_LATENCY;
			else latency += SSD_READ_LATENCY;
		}
	}
	free(temp);
	//get the first part of the last block
	if ((remaining != 0)&&(start_address/BLOCK_SIZE != (start_address+nbytes)/BLOCK_SIZE)) {
		if (disk_type==0) latency += HDD_SEEK+HDD_READ_LATENCY;
		else latency += SSD_READ_LATENCY;
		int last_block_index = (start_address+nbytes)/BLOCK_SIZE;
		char *last_block = (char *)malloc(BLOCK_SIZE);
		mydisk_read_block(last_block_index, last_block);
		char *last_block_cut = (char *)malloc(remaining+1);
		memcpy(last_block_cut, last_block, remaining);
		//memset(last_block_cut, '\0', sizeof(last_block_cut));
		//printf("last block cut: %s\n", last_block_cut);
		strcat(buffer, last_block_cut);
		free(last_block);
		free(last_block_cut);
	}
	report_latency(latency);

	return 0;
}

int mydisk_write(int start_address, int nbytes, void *buffer)
{
	//printf("writing: %s\n", buffer);
	int offset, remaining, amount, block_id, latency=0;
	int written=0;
	int cache_hit = 0, cache_miss = 0;
	if 	((start_address > max_blocks*BLOCK_SIZE)||
		(nbytes < 0)||(start_address < 0)||
		((start_address+nbytes) > max_blocks*BLOCK_SIZE))
	{
		//printf("bad arguments in mydisk_write\n");
		return 1;
	}
	if (nbytes==0) return 0; //nothing to do here, move along

	//printf("start address + nbytes: %d\n", start_address+nbytes);	
	offset = start_address%BLOCK_SIZE;
	remaining = (start_address+nbytes)%BLOCK_SIZE;
	amount = (BLOCK_SIZE - offset);
	//printf("off: %d rem: %d 1st block at: %d last block at: %d\n", offset, remaining, start_address/BLOCK_SIZE, (start_address+nbytes)/BLOCK_SIZE);
	int one_block = (start_address/BLOCK_SIZE == (start_address+nbytes)/BLOCK_SIZE);
	if ((offset!=0) || one_block) {
		if (disk_type==0) latency += HDD_SEEK+HDD_WRITE_LATENCY;
		else latency += SSD_WRITE_LATENCY;

		char *first_block = (char *)malloc(BLOCK_SIZE);
		char *new_first_block = (char *)malloc(BLOCK_SIZE);
		//writing all input to a single block
		mydisk_read_block(start_address/BLOCK_SIZE,first_block);
		//printf("original first block: %s\n",first_block); 
		memcpy(new_first_block, first_block, offset);//we cut the beginning off
		if (one_block) {
			memcpy(new_first_block+offset, buffer, nbytes);//copy buffer contents
			memcpy(new_first_block+offset+nbytes, first_block+offset+nbytes-1, remaining);//copy remainder of the original block back in
			mydisk_write_block(start_address/BLOCK_SIZE, new_first_block);
			report_latency(latency);
			return 0;
		}
		memcpy(new_first_block+offset, buffer, amount); //copy buffer content until end of block	
		//printf("modified block+144: %s\n", new_first_block+144);
		mydisk_write_block(start_address/BLOCK_SIZE, new_first_block);
		//printf("first block+offset should be%s\n", new_first_block+offset);
		written += amount;
		//printf("written to first block: %d\n", written);
		free(first_block);
		free(new_first_block);
	}
	int loops, loop_index;
	if (offset!=0) {
		loops = (nbytes-amount)/BLOCK_SIZE;
		loop_index = start_address/BLOCK_SIZE+1;
	}
	else {
		loops = nbytes/BLOCK_SIZE-1;
		loop_index = start_address/BLOCK_SIZE;
	}

	char *temp = (char *)malloc(BLOCK_SIZE);
	if (start_address/BLOCK_SIZE+1 < (start_address+nbytes)/BLOCK_SIZE) {
		int i, c;
		if (disk_type==0) latency += HDD_SEEK;
		//printf("loops: %d, loop_index: %d\n", loops, loop_index);
		for (i=0, c=loop_index; i<=loops; c++, i++) {
			if (disk_type==0) latency += HDD_WRITE_LATENCY;
			else latency += SSD_WRITE_LATENCY;
			memset(temp, 0, BLOCK_SIZE);
			memcpy(temp, buffer+written+i*BLOCK_SIZE, BLOCK_SIZE);//write from buffer to temp
			//printf("loopindex#%d i:%d \n", c, i);
			mydisk_write_block(c, temp);//write temp onto the file
			//printf("writing: %s\n", temp);
		}
		written += BLOCK_SIZE*(i);
		//printf("written value before last part: %d, final: %d\n", written, written+remaining);
	}
	free(temp);
	//get the first part of the last block
	if (remaining != 0) {
		if (disk_type==0) latency += HDD_SEEK+HDD_WRITE_LATENCY;
		else latency += SSD_WRITE_LATENCY;
		int last_block_index = (start_address+nbytes)/BLOCK_SIZE;
		char *last_block = (char *)malloc(BLOCK_SIZE);
		mydisk_read_block(last_block_index, last_block);
		//printf("original last block: %s\n", last_block);
		char *last_block_new = (char *)malloc(BLOCK_SIZE);
		memcpy(last_block_new, buffer+nbytes-remaining, remaining); //write the last part of buffer into the first part of the block
		memcpy(last_block_new+remaining, last_block+remaining, BLOCK_SIZE-remaining); //write back the second part of the original block
		//printf("new last block: %s\n", last_block_new);
		mydisk_write_block(last_block_index, last_block_new);
		//memset(last_block_cut, '\0', sizeof(last_block_cut));
		free(last_block);
		free(last_block_new);
	}
	report_latency(latency);
	return 0;
}
