valisp
======

A lispy flavour of Vala, written in Clojure. Don't ask why.

Running the Valisp compiler
---------------------------
Before everything, you must have installed leiningen and cloned the
git repository. Then:

```
lein run
```

should work. Except it won't do much, since it requires a source file
to compile it:

```
lein run some_file.valisp
```

This will print the resulting Vala code on stdout, so if you really
want to use this, you should do:

```
lein run some_file.valisp > some_file.vala
```

Hopefully you will get a valid Vala file, which you can now compile
(again) with 

```
valac some_file.vala
```

Obviously, it means you must have installed the Vala compiler, else it
is a bit pointless.

The valisp language
-------------------
First, let me remember you I said 'don't ask why'. Obviously, it is a
toy project, whose goal is to compile some lispy dialect to Vala.

A tiny example:

```
(defn main int []
  (stdout.printf "Please enter your name\n")
  (let [[name (stdin.read_line)]]
    (stdout.printf "Hello, %s\n" name)
    0))
```

You can look at [the current features list](doc/features.md) to see
which features are currently implemented. To be honest, they are
nearly all used in this tiny example, so don't get too much
expectations, right?

