local key = KEYS[1]
local now = tonumber(ARGV[1])
local window_start = tonumber(ARGV[2])
local max_requests = tonumber(ARGV[3])
local window_seconds = tonumber(ARGV[4])

-- Clean up scores older than window_start
redis.call('ZREMRANGEBYSCORE', key, 0, window_start)

-- Count current number of members in set
local current_requests = redis.call('ZCARD', key)

if current_requests < max_requests then
    -- Add the current timestamp as score and member
    redis.call('ZADD', key, now, tostring(now))
    -- Update expiration
    redis.call('EXPIRE', key, window_seconds)
    return 1
else
    return 0
end
