package com.domye.picture.core.result;

import lombok.Data;

import java.io.Serializable;

@Data
public class DeleteRequest implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    private Long id;
}