if(redis.call('get', Keys[1]) == ARGV[1]) then
    return redis.call('del', Keys[1])
end
return 0