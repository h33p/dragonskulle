
public interface IComponent {
    /** Gets called at a fixed interval */
    public void fixedUpdate(float deltaTime);
    /** Gets called every frame */
    public void renderUpdate(float deltaTime);
}
