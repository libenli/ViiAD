package com.mrd.ad.business.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mrd.ad.business.system.domain.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("select * from sys_user where deleted = 0 and status = 'active' and (username = #{target} or email = #{target}) limit 1")
    SysUser findActiveByEmailOrUsername(@Param("target") String target);

    @Select("select * from sys_user where deleted = 0 and status = 'active' and (phone = #{phone} or phone = concat(#{countryCode}, ' ', #{phone}) or phone = concat(#{countryCode}, #{phone})) limit 1")
    SysUser findActiveByPhone(@Param("countryCode") String countryCode, @Param("phone") String phone);
}
