#pragma once
#include <stdint.h>
#include <linux/ioctl.h>
#include <linux/types.h>

typedef uint64_t binder_size_t;
typedef uint64_t binder_uintptr_t;

struct binder_write_read {
    binder_size_t    write_size;
    binder_size_t    write_consumed;
    binder_uintptr_t write_buffer;
    binder_size_t    read_size;
    binder_size_t    read_consumed;
    binder_uintptr_t read_buffer;
};

struct binder_version { int32_t protocol_version; };

struct binder_transaction_data {
    union { uint32_t handle; binder_uintptr_t ptr; } target;
    binder_uintptr_t cookie;
    uint32_t code;
    uint32_t flags;
    int32_t  sender_pid;
    uint32_t sender_euid;
    binder_size_t data_size;
    binder_size_t offsets_size;
    union {
        struct { binder_uintptr_t buffer; binder_uintptr_t offsets; } ptr;
        uint8_t buf[8];
    } data;
};

#define BINDER_WRITE_READ      _IOWR('b', 1, struct binder_write_read)
#define BINDER_SET_MAX_THREADS _IOW('b',  5, uint32_t)
#define BINDER_VERSION         _IOWR('b', 9, struct binder_version)

#define BC_TRANSACTION _IOW('c', 0, struct binder_transaction_data)
#define BC_FREE_BUFFER _IOW('c', 3, binder_uintptr_t)
#define TF_ONE_WAY     0x01
