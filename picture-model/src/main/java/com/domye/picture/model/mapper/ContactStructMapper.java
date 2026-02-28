package com.domye.picture.model.mapper;

import com.domye.picture.model.entity.contact.Contact;
import com.domye.picture.model.vo.contact.ContactVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ContactStructMapper {
    ContactStructMapper INSTANCE = Mappers.getMapper(ContactStructMapper.class);
    
    ContactVO toVo(Contact contact);
}
