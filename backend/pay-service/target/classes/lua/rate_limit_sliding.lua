local key       = KEYS[1]
local limit     = tonumber(ARGV[1])
local window_ms = tonumber(ARGV[2])
local now       = tonumber(ARGV[3])
local expire    = now - window_ms
redis.call('ZREMRANGEBYSCORE', key, '-inf', expire)
local count = redis.call('ZCARD', key)
if tonumber(count) >= limit then
    return 0
end
redis.call('ZADD', key, now, now .. math.random(1, 999999))
redis.call('PEXPIRE', key, window_ms)
return 1
