## The Game of Life in a Time-symmetrical Universe

Can the Game of Life (GoL) emerge in a time-symmetrical universe? Is it possible for a reversible cellular automaton (CA)
to somehow emulate it? One aspect of time irreversibility of GoL is that it loses information. 
For one thing, there are many configurations of alive cells that are far enough from each other so that the entire 
population dies out in the next generation. 
To be able to go back in time from an empty space one would need to preserve information lost in the GoL state.

While there are several clever tricks how to go about preserving that information, one rather straightforward solution 
is to add a new dimension and use it to go back and forth in time as previous generations are reconstructed 
or new generations come into existence. 
A static _block universe_ is an obvious solution. Basically, every generation is cached in the added dimension and 
is fetched upon request as we simulate time going backwards. 
It's quite dull, though, and the procedure to produce the GoL state when reversing time is quite different from that 
of producing new generations.

Naturally, one would like to see essentially the same CA rule to be applied to the 3D state when we move in both 
time directions. There is a subtlety about the last statement as from the 3D CA point of view there will be no 
difference in how it works, i.e. _its time_ will always move forward. 
It's how the state of GoL changes that will create a perception of time, that is _GoL time_, going forward or backward. 
Or in some other direction.

Let's get technical. GoL can be generally described as a 2-dimensional CA with the change rule

_S(r,t+1) = F({S(r*,t)})_,

where _S(r,t)_ is the state, dead or alive, of a cell at location _r_ and time _t_; _F_ - an arbitrary function that 
takes states of a set of cells _r*_, usually neighbors of _r_, as arguments.  
When we implement the rule in a computer program we usually find it necessary to keep two state arrays for generation 
_t_, whose state values are known, and generation _t+1_ whose state values we compute. 
Once generation _t+1_ is complete, we may discard all the values from the first array and reuse it for generation _t+2_ 
to be computed next. 
This array swapping is a usual technique of how to implement progressing in time for an arbitrary CA. 
Forward in time, that is.

Function _F_ in general is not reversible but even when it is, going from generation _t+1_ back to generation _t_ may be 
a formidable computational task requiring solving a system of equations for the entire set of cells in the generation.
There is a simple trick, however, that allows us to introduce and easily implement a general class of reversible automata. 
Let's not discard state values of generation _t_ in the scheme above but rather update them in a reversible way:

_S(r,t+1) = S(r,t-1) ^ F({S(r*,t)})_ 

The ^ operation can be any reversible binary operation but since GoL states are bits, XOR fits in naturally. 
If we keep two generations, _t-1_ and _t_, and go with the formula, we can compute generation _t+1_ in place 
substituting _S(r,t-1)_ values without interfering with neighboring computations. 
If, however, we swap the two generations, the same formula will reconstruct generation _t-2_ in place of 
generation _t_:

_S(r,t-2) = S(r,t) ^ F({S(r*,t-1)})_

How to use this approach to make GoL reversible in a three-dimensional space? 
First, let's choose one dimension, say _z_, to represent time. 
In the otherwise empty space, set the plane _(x, y, 1)_ at time _1_ to a desired GoL state. 
Start applying the reversible formula above using the same GoL rule. 
In every new generation _t_, a valid GoL state will be computed in plane _(x, y, t)_. 
If swapping of the two arrays at some moment is skipped, the GoT time effectively reverses and goes back to the original 
state. Can you go beyond it? What could stop you? 
   
![Time-reversible Game of Life](images/janus0.gif?raw=true)   

### Anisotropy

My selection of the "time" dimension was arbitrary. I could choose _x_, _y_, or _-z_. The _F_ function would change accordingly.
Now my question is, can the _F_ function be defined universally so that it would work for any of the 6 directions?  
That would be a 3-D CA with up to 27 cells determining the new state of a cell on hand. It's not difficult to see that 
given an arbitrary state of those 27 cells one could figure out whether all alive cells are in only one face of the cube
(if not, the new state can be defined arbitrarily) and compute the new state of the center cell according to the rule of GoL.
Due to symmetry, there are no collisions when all alive cells belong to two faces (i.e. an edge) or three faces
(i.e. a vertex) - the result is the same.  
Knowing that the answer to the question is positive, my main concern becomes purely technical: how to avoid having to deal
with a large lookup table or some convoluted algorithm that determines whether all alive cells lie on one face and which.
I want a simple and elegant formula and I believe I got one.

I start with formulating the rule of GoL in the following form:       

Sum over 8 neighbors: |  0  |  1  |   2  |  3  |  4  |  5  |  6  |  7  |  8
--------------------- | --- | --- | ---- | --- | --- | --- | --- | --- | ---
**New state:**        |**0**|**0**|**same**|**1**|**0**|**0**|**0**|**0**|**0**

Here, **same** means keep the same state: if there is a dead cell with two alive neighbors, the new state will be 0 (still dead);
an alive cell with two alive neighbors will retain its state 1 (still alive).  
My first step towards generalization is to include the state of the center cell, i.e. count all 9 cells:

Sum over 9 cells: |  0  |  1  |   2  |  3  |  4  |  5  |  6  |  7  |  8 |  9
----------------- | --- | --- | ---- | --- | --- | --- | --- | --- | --- | ---   
**New state:**    |**0**|**0**|**0**|**1**|**same**|**0**|**0**|**0**|**0**|**0** 

You can verify for every case that it's the same Game of Life.  
It's time to add the third dimension. All cases but 4 can be generalized straightforwardly:

Sum over 27 cells: |  0  |  1  |   2  |  3  |  4  |  5  |  6  |  7  |  8 |  9 | ... | 27
------------------ | --- | --- | ---- | --- | --- | --- | --- | --- | --- | --- | --- | ---  
**New state:**     |**0**|**0**|**0**|**1**|**same¹**|**0**|**0**|**0**|**0**|**0**|...|**0** 
 
¹ - the state of the center cell of the face to which all four alive cells belong, if any.  

When iterating over the 27 cells, one can assign to each three relative coordinates: _-1, 0, 1_.
The center cell will be _(0, 0, 0)_. One vertex will be _(-1, -1, -1)_ and the opposite - _(1, 1, 1)_.
When a set of cells lies on the same face, they share the same non-zero value for some coordinate, f.e.
_(1, 0, -1), (1, -1, 1), (1, 0, 0), (1, 1, 1)_ - all lie on the same face _x=1_.  
And here is the idea: if I compute the average of a set of such coordinates it will be exactly _-1_ or _1_ 
only when all coordinates in the set are _-1_ or _1_ correspondingly (and I don't care about _0_'s). Conversion of double values to integers 
as implemented in Java (or C for that matter) comes very handy given the fact that positive doubles are rounded down
and negative doubles are rounded up (for once it's useful). You can verify that

_(int)((x₀ + x₁ + x₂ + x₃) / 4.)_  

satisfies the desired property. It's simple, symmetrical, and, one might even say, elegant.  

As a short cut for the 3D time-symmetrical CA with special treatment of 3 and 4 alive predecessors, I'll use T-34.
Here, I created a special T-34 initial state that seeds the _acorn_ GoL pattern in all 6 directions.
It starts expanding but at some moment _collides with a heavenly axis_ and reverses its course to return to its original state, whence the cycle repeats.
Only 10,000 alive cells are shown. When running the actual Java program, you can rotate the image, zoom in and out, pause and record a snapshot.

![Anisotropic Game of Life](images/janus1.gif?raw=true)   

The rule is stateless. The state rules.  


### Reflections

The Game of Life, as arguably the most widely known cellular automaton, has been a useful tool to think about big questions: determinism, free will, do we live in Matrix?, etc.
Extending the model surely should extend its applicability to no lesser questions.

_Can the Arrow of Time have a direction? Be actually an arrow?_

I believe GoL in T-34 gives a hint at how to interpret that. The 6 discrete directions are a far cry from a smooth manifold 
but it's a step in the right direction.  

_Is GoL emergent in the T-34 universe?_ 

Sure. If somebody formulated T-34 first and then discovered GoL as the state 
of an expanding surface, there would be no doubt about it.

_Is GoL reducible to T-34?_ 

Yes. There is no truth about GoL that is not also a truth about T-34. GoL supervenes on T-34, if you wish. But now there is 
a twist: there is also no use in that reduction as there will be no new truth about GoL that the new framework 
could possibly provide (it could provide new ways to discover new truths about GoL, but that's it).
GoL is self-sufficient. It's a virtual machine and that it can be implemented in a different virtual machine 
does not add nor remove anything from it (and, you know, there are virtual machines all the way down). 
It's totally isolated (abstracted) from its realization (implementation).


A genius once said: _Science is a differential equation. Religion is a boundary condition_. 
That was before there were popular movies about computers and cellular automata. 
Unavoidably, it created confusion. 
Authentic or not (or whether it's serious or not), the quote remains open to interpretations and here is mine:
we may know the laws of motion (the rule) but the actual picture is determined by boundary conditions 
(the initial state, there are no space boundaries in my universe) and it is not limited by the rigidness of the laws 
(there is a universal machine in GoL, and T-34 by inclusion, after all).  

##### Disclaimer: this is a work of fiction. Names, characters, strings and lines, states and events are the products of the author's imagination. Any resemblance to actual cells, living or dead, or actual events is purely coincidental.   
