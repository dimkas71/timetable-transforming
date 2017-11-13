package ua.compservice.model;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.List;


@Value
@AllArgsConstructor
public class ImmutableRow {

    private int row;
    private List<ImmutableCell> cells;

}
