#include <jni.h>

#define TAG "NetGuard.JNI"

#define SELECT_TIMEOUT 10 // seconds

#define TUN_MAXMSG 32768 // bytes (device)
#define UDP4_MAXMSG 65507 // bytes (socket)

#define UDP_TTL 64
#define UDP_TIMEOUT_53 15 // seconds
#define UDP_TIMEOUT_ANY 300 // seconds

#define TCP_TTL 64
#define TCP_RECV_WINDOW 2048 // bytes
#define TCP_SEND_WINDOW 2048 // bytes (maximum)
#define TCP_INIT_TIMEOUT 30 // seconds ~net.inet.tcp.keepinit
#define TCP_IDLE_TIMEOUT 300 // seconds ~net.inet.tcp.keepidle
#define TCP_CLOSE_TIMEOUT 30 // seconds
#define TCP_KEEP_TIMEOUT 300 // seconds

#define UID_DELAY 1 // milliseconds
#define UID_DELAYTRY 10 // milliseconds
#define UID_MAXTRY 3

#define MAX_PCAP_FILE (1024 * 1024) // bytes
#define MAX_PCAP_RECORD 128 // bytes

struct arguments {
    JNIEnv *env;
    jobject instance;
    int tun;
    jint ucount;
    jint *uids;
    int hcount;
    char **hosts;
    jboolean log;
    jboolean filter;
};

struct udp_session {
    // TODO UDPv6
    time_t time;
    jint uid;
    int version;

    union {
        __be32 ipv4; // network notation
    } saddr1;
    __be16 source; // network notation

    union {
        __be32 ipv4; // network notation
    } daddr1;

    __be16 dest; // network notation
    uint8_t stop;
    jint socket;
    struct udp_session *next;
};

struct tcp_session {
    // TODO TCPv6
    jint uid;
    time_t time;
    int version;
    uint16_t send_window; // host notation

    uint32_t remote_seq; // confirmed bytes received, host notation
    uint32_t local_seq; // confirmed bytes sent, host notation
    uint32_t remote_start;
    uint32_t local_start;

    union {
        __be32 ipv4; // network notation
    } saddr;
    __be16 source; // network notation

    union {
        __be32 ipv4; // network notation
    } daddr;
    __be16 dest; // network notation

    uint8_t state;
    jint socket;
    struct tcp_session *next;
};

// https://wiki.wireshark.org/Development/LibpcapFileFormat

typedef uint16_t guint16_t;
typedef uint32_t guint32_t;
typedef int32_t gint32_t;

typedef struct pcap_hdr_s {
    guint32_t magic_number;
    guint16_t version_major;
    guint16_t version_minor;
    gint32_t thiszone;
    guint32_t sigfigs;
    guint32_t snaplen;
    guint32_t network;
} __packed;

typedef struct pcaprec_hdr_s {
    guint32_t ts_sec;
    guint32_t ts_usec;
    guint32_t incl_len;
    guint32_t orig_len;
} __packed;

#define LINKTYPE_RAW 101

typedef struct dns_header {
    __be16 msgid;
    __be16 flags;
    __be16 qdcount;
    __be16 ancount;
    __be16 nscount;
    __be16 arcount;
} __packed;

typedef struct dns_response {
    __be16 qname_ptr;
    __be16 qtype;
    __be16 qclass;
    __be32 ttl;
    __be16 rdlength;
    __be32 rdata;
} __packed;

#define DNS_QR 1
#define DNS_OPCODE 30
#define DNS_TC 8

#define DNS_QTYPE_A 1 // IPv4
#define DNS_QTYPE_AAAA 28 // IPv6
#define DNS_QCLASS_IN 1

#define DNS_TTL 3600 // seconds

void check_allowed(const struct arguments *args);

void clear_sessions();

void handle_signal(int sig, siginfo_t *info, void *context);

void *handle_events(void *a);

void report_exit(const struct arguments *args, const char *reason);

void check_sessions(const struct arguments *args);

int get_selects(const struct arguments *args, fd_set *rfds, fd_set *wfds, fd_set *efds);

int check_tun(const struct arguments *args, fd_set *rfds, fd_set *wfds, fd_set *efds);

void check_udp_sockets(const struct arguments *args, fd_set *rfds, fd_set *wfds, fd_set *efds);

void check_tcp_sockets(const struct arguments *args, fd_set *rfds, fd_set *wfds, fd_set *efds);

void handle_ip(const struct arguments *args, const uint8_t *buffer, size_t length);

jboolean handle_udp(const struct arguments *args, const uint8_t *buffer, size_t length, int uid);

jboolean handle_tcp(const struct arguments *args, const uint8_t *buffer, size_t length, int uid);

int check_dns(const struct arguments *args, const struct udp_session *u,
              const uint8_t *buffer, const size_t length);

int open_socket(const struct tcp_session *cur, const struct arguments *args);

int32_t get_local_port(const int sock);

int write_syn_ack(const struct arguments *args, struct tcp_session *cur);

int write_ack(const struct arguments *args, struct tcp_session *cur, size_t bytes);

int write_data(const struct arguments *args, struct tcp_session *cur,
               const uint8_t *buffer, size_t length);

int write_fin_ack(const struct arguments *args, struct tcp_session *cur, size_t bytes);

void write_rst(const struct arguments *args, struct tcp_session *cur);

ssize_t write_udp(const struct arguments *args, const struct udp_session *cur,
                  uint8_t *data, size_t datalen);

ssize_t write_tcp(const struct arguments *args, const struct tcp_session *cur,
                  const uint8_t *data, size_t datalen, size_t confirm,
                  int syn, int ack, int fin, int rst);

uint8_t char2nible(const char c);

void hex2bytes(const char *hex, uint8_t *buffer);

jint get_uid(const int protocol, const int version,
             const void *saddr, const uint16_t sport, int dump);

int protect_socket(const struct arguments *args, int socket);

uint16_t calc_checksum(uint16_t start, const uint8_t *buffer, size_t length);

jobject jniGlobalRef(JNIEnv *env, jobject cls);

jclass jniFindClass(JNIEnv *env, const char *name);

jmethodID jniGetMethodID(JNIEnv *env, jclass cls, const char *name, const char *signature);

jfieldID jniGetFieldID(JNIEnv *env, jclass cls, const char *name, const char *type);

jobject jniNewObject(JNIEnv *env, jclass cls, jmethodID constructor, const char *name);

int jniCheckException(JNIEnv *env);

void log_android(int prio, const char *fmt, ...);

void log_packet(const struct arguments *args,
                jint version,
                jint protocol,
                const char *flags,
                const char *source,
                jint sport,
                const char *dest,
                jint dport,
                jint uid,
                jboolean allowed);

void write_pcap_hdr();

void write_pcap_rec(const uint8_t *buffer, size_t len);

void write_pcap(const void *ptr, size_t len);

void read_hosts(const char *name, struct arguments *args);

const char *strstate(const int state);

char *hex(const u_int8_t *data, const size_t len);
