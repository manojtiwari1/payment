package com.app.modules.picture.model;

import com.app.common.enums.PictureType;
import com.app.common.model.AbstractAuditableModel;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name="picture")
public class Picture extends AbstractAuditableModel implements Serializable {

    @Enumerated(EnumType.STRING)
    private PictureType type;

    private String url;

    public Picture(String url) {
        this.url = url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Picture picture = (Picture) o;
        return url.equals(picture.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }
}

