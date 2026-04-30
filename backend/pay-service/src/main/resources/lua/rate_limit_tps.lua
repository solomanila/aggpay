local key     = KEYS[1]
local limit   = tonumber(ARGV[1])
local current = redis.call('INCR', key)
if current == 1 then
    redis.call('EXPIRE', key, 1)
end
if current > limit then
    return 0
end
return 1
