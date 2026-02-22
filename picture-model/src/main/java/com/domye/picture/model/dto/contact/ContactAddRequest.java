package com.domye.picture.model.dto.contact;

import lombok.Data;

import java.io.Serializable;

@Data
public class ContactAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long contactUserId;
}