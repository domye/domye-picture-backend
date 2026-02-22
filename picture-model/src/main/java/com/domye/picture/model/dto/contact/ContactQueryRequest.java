package com.domye.picture.model.dto.contact;

import com.domye.picture.common.result.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
public class ContactQueryRequest extends PageRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private Integer status;
}