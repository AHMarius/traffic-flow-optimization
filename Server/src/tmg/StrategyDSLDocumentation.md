# Strategy DSL

```
increase <target> by|factor <value>
decrease <target> by|factor <value>
set <target> <value>
random <target> [source]
```

**target**: `all` | `random` | `<X,Y,Z>`
**value**: a number, or `random (min,max)`
**source**: `[min,max]` / `(min,max)` interval | `values [1,2,3]` | `random` (or omitted) = copy from an existing param

## Math

| | by | factor |
|---|---|---|
| increase | `+ amount` | `* amount` |
| decrease | `- amount` | `/ amount` |

`set` just assigns. `random` assigns a value drawn from `source`.

## Examples

```
increase <X> factor 2
decrease all by 3
set <X,Y> 4
random <X,Y> [0,4]
random all values [1,2,3]
random random
```