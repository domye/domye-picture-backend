package com.domye.picture.model.mapper;

import com.domye.picture.model.entity.contact.Contact;
import com.domye.picture.model.vo.contact.ContactVO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ContactStructMapper {
    ContactVO toVo(Contact contact);
}
