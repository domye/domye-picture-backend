package com.domye.picture.model.vo.picture;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class PictureGalleryVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private List<PictureVO> records;
}