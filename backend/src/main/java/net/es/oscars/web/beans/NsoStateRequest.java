package net.es.oscars.web.beans;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NsoStateRequest {
    @JsonSetter(nulls = Nulls.SKIP)
    @Builder.Default
    public List<Integer> vcIds = new ArrayList<>();
}

