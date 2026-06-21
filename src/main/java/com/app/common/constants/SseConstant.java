package com.app.common.constants;

/**
 * @author Manoj Tiwari
 * @since 14-04-2026
 * @implNote Constants related to Server-Sent Events (SSE) configuration and usage.
 * This class defines constants for stream name, consumer group, batch size, timeouts, and other
 * parameters used across the SSE implementation. These constants help maintain consistency and make
 * it easier to manage configuration values in one place. For example, STREAM_NAME defines the name of the
 * Redis stream used for SSE events, while SSE_GROUP_NAME and SSE_CONSUMER_NAME specify the consumer group
 * and consumer name for consuming events. Batch size and timeout constants control how events are read from
 * the stream, and heart beat interval and stream length constants help manage the lifecycle and performance
 * of the SSE system.
 */

public class SseConstant {

    public static final String STREAM_NAME = "ses-stream";

    public static final String SSE_GROUP_NAME = "ses-group";

    public static final String SSE_CONSUMER_NAME = "instance-local";

    public static final int SSE_BATCH_SIZE = 50;

    public static final long SSE_BLOCK_TIMEOUT_MS = 2000;

    public static final long SSE_TIMEOUT_MS = 0;

    public static final int SSE_HEART_BEAT_INTERVAL = 25;

    public static final long SSE_MIN_IDLE_INTERVAL = 30_000;

    public static final long SSE_MAX_STREAM_LENGTH = 10_000;

}
