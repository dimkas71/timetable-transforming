package ua.compservice.model;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

@Value
@AllArgsConstructor
public class Cell {

    private final @NonNull int row;
    private final @NonNull int column;
    private final @NonNull String value;

}
