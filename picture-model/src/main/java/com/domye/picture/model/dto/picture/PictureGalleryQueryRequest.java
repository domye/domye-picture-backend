package com.domye.picture.model.dto.picture;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;

@Data
public class PictureGalleryQueryRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String userAccount;

    private long current = 1;

    private long size = 20;
}
