package com.example.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class TypeValue {

    private String type;

    private Integer count = 0;

    private Integer sumLevel = 0;

    private Integer sumValue = 0;


    public static TypeValue getOne(Collection<TypeValue> typeValues,String type){

        for (TypeValue typeValue : typeValues) {
            if (type.equals(typeValue.getType())){
                return typeValue;
            }
        }

        return new TypeValue().setType(type);
    }


}
