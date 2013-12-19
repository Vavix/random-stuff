#include "mydisk.h"
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#define MAX_BLOCKS 16
#define CACHED_BLOCKS 4
#define DISK_CAPACITY ((MAX_BLOCKS)*(BLOCK_SIZE))

static char buffer[MAX_BLOCKS * BLOCK_SIZE];
static char buffer2[MAX_BLOCKS * BLOCK_SIZE];

void report_latency(int latency)
{
	/* TODO: If debugging, uncomment the below line to see the latency */
	printf("Operation latency: %d\n", latency);
}

void rand_str(char *buf, int size)
{
	int i;
	for (i = 0; i < size; ++i) {
		buf[i] = '0' + rand() % 9;
	}
}

int stress_test() {
	int i, id;
	static char dataset[MAX_BLOCKS][BLOCK_SIZE];
	char tmp[BLOCK_SIZE];
	
	for (i = 0; i < MAX_BLOCKS; ++i) {
		rand_str(dataset[i], BLOCK_SIZE);
	}

	for (i = 0; i < 10000; ++i) {
		id = rand() % MAX_BLOCKS;
		mydisk_write_block(id, dataset[id]);
	}

	for (i = 0; i < MAX_BLOCKS; ++i) {
		mydisk_read_block(i, tmp);
		if (memcmp(dataset[i], tmp, BLOCK_SIZE)) {
			return 1;
		}
	}

	return 0;
}

int stress_test2()
{
	int start, end, tmp, size;
	int i;

	for (i = 0; i < 10000; ++i) {
		start = rand() % (MAX_BLOCKS * BLOCK_SIZE);
		end = rand() % (MAX_BLOCKS * BLOCK_SIZE);
		if (start > end) {
			tmp = start;
			start = end;
			end = tmp;
		}
		size = end - start;
		rand_str(buffer, size);
		mydisk_write(start, size, buffer);
		mydisk_read(start, size, buffer2);
		if (memcmp(buffer, buffer2, size)) {
			return 1;
		}
	}
	return 0;
}

void check_test(int v) {
	if (v) {
		printf("FAILED\n");
	} else {
		printf("PASS\n");
	}
}

int main()
{
	int size;

	mydisk_init("diskfile", MAX_BLOCKS, 0);
//	init_cache(CACHED_BLOCKS);
	
//	/* Test case 1: read/write block */
	size = BLOCK_SIZE;
	memset(buffer, 0, size);
	strcpy(buffer, "hello world\n");
	mydisk_write_block(0, buffer);
	memset(buffer2, 0, size);
	mydisk_read_block(0, buffer2);
	check_test(memcmp(buffer2, "hello world\n", 13));
	mydisk_write_block(1, buffer);
//
//	/* Test case 2: basic read/write */
	memset(buffer, 0, size);
	mydisk_read(0, 13, buffer);
	check_test(memcmp(buffer, "hello world\n", 13));

 	/* Test case 3: read in the middle */
	memset(buffer, 0, BLOCK_SIZE);
	mydisk_read(8, 5, buffer);
	check_test(memcmp(buffer, "rld\n", 5));

	/* Test case 4: read/write across blocks */
	size = BLOCK_SIZE;
	rand_str(buffer, size);
	mydisk_write(144, size, buffer);
	memset(buffer2, 0, size);
	mydisk_read(144, size, buffer2);
	check_test(memcmp(buffer, buffer2, size));

	/* Test case 5: large read/write */
	size = BLOCK_SIZE * (MAX_BLOCKS - 1);
	rand_str(buffer, size);
	mydisk_write(276, size, buffer);
	mydisk_read(276, size, buffer2);
	check_test(memcmp(buffer, buffer2, size));

	/* Test case 6~9: read/write exception */
	check_test(!mydisk_read(-1, 0, buffer));
	check_test(!mydisk_read(0, -10, buffer));
	check_test(!mydisk_read(100, BLOCK_SIZE * MAX_BLOCKS, buffer));
	check_test(mydisk_write(0, 0, buffer));

	check_test(stress_test());
	//check_test(stress_test2());

	//close_cache();
	mydisk_close();
	return 0;
}


