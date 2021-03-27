# How to use:
All input can be accessed statically via `GameActions`.

<hr />

### Example: I want to add a new action called `GREETINGS`:
- In `GameActions` add: `public static final Action GREETINGS = new Action();` 
  - If you want to make debugging easier, you can give the action a display name: `public static final Action GREETINGS = new Action("DISPLAY_NAME");`
- To access this action in your code, you simply do `GameActions.GREETINGS`.
  - At the moment, you can currently only see if the action is activated or not at that point in time- using `isActivated()`.
      For example, inside your update function you could do:
      ``` java
      if(GameActions.GREETINGS.isActivated()) { 
        System.out.println("Greetings!");
      }
      ```
      This would print "Greetings" if `GREETINGS` is activated at that point in time.

However, `GREETINGS` is not activated by anything at the moment. We can change that by adding a binding.

<hr />

### Example: I want be able to activate my `GREETINGS` action (by adding a binding):
- In the constructor of `GameBindings`, simply use `addBinding` to add a new binding between a button and the desired action.
- You need to decide which button should activate `GREETINGS`.
  - There are loads of keyboard and mouse buttons to choose from. See https://www.glfw.org/docs/latest/group__keys.html and https://www.glfw.org/docs/latest/group__buttons.html for all of the options. 
  - Furthermore, there are some special custom made buttons that you can use in the form of `Scroll.UP` and `Scroll.DOWN`. When the mouse wheel scrolls up or down, these custom buttons are pressed (respectively). This is just one of the ways to access scrolling (you can also access the raw scrolling value- see 'Other things `GameActions` can do').
- If you decide you want to map the 'G' keyboard button to our `GREETINGS` action, you first need to get the GLFW button code. In this case, it would be `GLFW.GLFW_KEY_G`.
    ``` java
    addBinding(GLFW.GLFW_KEY_G, GameActions.GREETINGS);
    ```
    Now whenever the 'G' key is pressed on the keyboard, `GREETINGS` is activated.
- If you decide that you also want the left mouse button to activate `GREETINGS`, you could simply add another binding:
    ``` java
    addBinding(GLFW.GLFW_MOUSE_BUTTON_LEFT, GameActions.GREETINGS);
    ```
Now whenever either the 'G' key is pressed, or the left mouse button is pressed, `GREETINGS` is activated.

<hr />

### Other things `GameActions` can do:
- `GameActions` can also be used to access the cursor and mouse scrolling.
  - `GameActions.getCursor()` allows you to access the cursor data. For example, you can see the current cursor position, whether it is being dragged, the distance of drag, the angle of drag, etc.
  - `GameActions.getScroll()` can be used to access raw scrolling values.`getAmount()` allows you to see how much the scroll wheel has changed since the last time input was polled.

<hr />

### More on `Bindings`:
- To get the current `Bindings` being used, simply call `Input.getBindings()`. 
- You can dynamically add and remove bindings, via `Bindings`.
  - `addBindings(button, action1, action2, ...)` can be used to add bindings to a button.
  - `removeBinding(button);` can be used to remove all bindings for a button.
