package com.domye.picture.model.dto.contact;

import lombok.Data;

import java.io.Serializable;

@Data
public class ContactHandleRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String status;
}