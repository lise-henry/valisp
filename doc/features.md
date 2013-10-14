Features
========

Currently implemented
---------------------

## Function/method calls ##

You just call a function with:

```
(function arg1 ... argn)
```

It is possible to call every function or method you could call in
Vala. For objects, just use the `object.method` form:

```
(stdout.printf "Hello, world!\n")
```

Currently, there is absolutely no translation of function names,
meaning: 

* you can't declare function names with `?`, `!`, or `-` characters as
  you would in the lisp way;
* you must call functions_with_underscores with underscores and not as
  function-with-underscores.
  
## Accessing arrays ##

You can access the element `i` of an array 

```
(i array)
```

For example to print the name of the program:

```
(stdout.printf (0 args))
```

## Defining arrays ##

Arrays can be defined with `[]`:

```
[1 2 3]
```

Empty arrays can also be constructed with the `array` form:

```
(array int 10)
```

## set! ##

To assign a value to a variable (mutating it), you can use `set!`:

```
(set! x 42)
```

This also works for arrays:

```
(set! (3 x) 42)
```

translates to

```
x[3] = 42
```

## if ##

If is implemented and can work with three params or two params:

```
(if condition
    expr-true
    expr-false)
```

```
(if condition
    expr-true)
```

It is possible to use the return value of `if`, but only with the
thee-params version:

```
(stdout.printf 
    (if (is-french)
        "Bonjour le monde\n"
        "Hello, world\n"))
```


## defn ##

Generate a top-level function
```
(defn name type [param1 type1 ...]
    expr)
```
Other forms accepted are: 

Multiple expressions in function body:
```
(defn name type [param1 type1 ...]
    expr1
    expr2)
```

Implicit void return type:
```
(defn name [params]
    expr)
```

Implicit function of no argument:
```
(defn name type
    expr)
```

Both implicit void input and return type:
```
(defn name
    expr)
```

Note, though, that multiple expressions in the body are only possible
with full declaration.

## fn ##

Generates a closure or anonymous function. The syntax is similar to
`defn`, except:

* there is no name, because it is anonymous ;
* there is no need for the type of the parameters, since it can be
  inferred (the return type is still needed, though).
  
So it looks like:

```
(fn int [x]
    (* x 2))
```

## types ##

Since it's very directly compiled to vala and uses no run-time, Valisp
needs to be statically typed, which is a bit unusual for lispy syntax.

Types are the same as their Vala counterpart, except:

* an array of type T is `<T>` (and not `T[]`)
* types for function pointers are not yet implemented.

## comparators ##

`<`, `>`, `=`, `not=`, `<=`, `>=` all work as you would suppose they do, eg:

```
(< 2 3)
```

returns true.

They can take more than 2 arguments:

```
(< 0 x 10)
```

## arithmetic operators ##

Standard arithmetic operators, `+`, `-`, `*`, `/` work in the same
way: 

```
(+ 3 4 5)
```

## let ##

`let` lets (ah, ah) you binds some values to some variables, and to
evaluate sequentially a list of expressions, only returning the value
of the last one:

```
(let [[var1 type1 value1]
      [var2 value2]]
     expr1
     ...
     exprn)
```

Types are optional: if not given, `var` will be used, hoping the vala
compiler can infer the type from the value. So

```
(let [[x 42]]
     x)
```

should work, while

```
(let [[x null]]
     x)
```

will probably not.

## do ##

`do` allows you to evaluate sequentially multiple expressions, only
returning the value of the last one:

```
(do
    expr1
    ...
    exprn)
```

Not yet inplemented
-------------------

* types for function pointers
* loops
* lists
* vectors
* classes and methods definition
* a valisp compiler in valisp
