import java.awt.*;
import java.text.NumberFormat;
import java.util.Vector;

class CollisionException extends RuntimeException {
    public Vector collisions = null;

    public CollisionException(Vector collisions) {
        super();
        this.collisions = collisions;
    }
}

//////////////////////////////////////////////////////////////////////////
/*  CollidingSim handles collisions.
*/
public abstract class CollidingSim extends Simulation {
    protected double lastTimeStep = 0;
    //double TOL = 0.03;  // collision tolerance (accuracy) in seconds
    double TOL = 0.0001;  // keep small! see note on 3 BODY COLLISION BUG in Thruster5
    //double TOL = 0.001;
    private double[] old_vars = null;  // keep this array to prevent memory reallocation
    private NumberFormat dnf = NumberFormat.getNumberInstance();

    public CollidingSim(Container container) {
        super(container);
    }

    public CollidingSim(Container container, int numVars) {
        super(container, numVars);
    }

    private void debugPrint(int k, Vector collisions) {
        switch (k) {
            case 0:
                dnf.setMaximumFractionDigits(7);
                String s = "starting vars";
                for (int i = 0; i < vars.length; i++)
                    s += " [" + i + "]=" + dnf.format(vars[i]);
                System.out.println(s);
                break;
            case 1:
                if (collisions != null)
                    for (int i = 0; i < collisions.size(); i++) {
                        System.out.println("[" + i + "] " + collisions.elementAt(i));
                    }
                break;
        }
    }

    /*
      Advance the simulation by the requested timeStep, handling any
      collisions along the way by using binary search to get close to
      the time of collision, handling the collision, then continuing on.

      We try to make sure we advance the full timeStep requested, unless
      we get stuck in a loop.

      How it works:
      We try to advance by a certain timestep.  If there are collisions
      at that time, we back up and try again.  The abstract method
      findAllCollisions() returns a Vector of collisions, or null if
      no collisions.  Continue with binary search
      till we are just before the time of the collision ("just before"
      being determined by this.TOL parameter.)  Then pass the last
      Vector of collision data (actually from the very near future) to
      the application in handleCollisions.  The application adjusts
      the simulation accordingly.  Now advance the simulation the remaining
      time left in the requested timeStep.  Again, if more collisions
      are encountered in this timestep we go through binary search
      and handleCollisions.
    */
    protected void advance(double timeStep) {
        double timeAdvanced = 0; // how much time simulation has advanced
        double h = timeStep;
        int collisionCount = 0;  // for detecting 'stuck' condition
        while (timeAdvanced < timeStep) {
            // allocate space for old_vars if necessary
            if (old_vars == null || old_vars.length < vars.length)
                old_vars = new double[vars.length];
            // save variables
            for (int i = 0; i < vars.length; i++)
                old_vars[i] = vars[i];
            Vector collisions = null;
            try {
                odeSolver.step(h);
                modifyObjects();
                collisions = findAllCollisions();
            } catch (CollisionException e) {
                collisions = e.collisions;
            }
            if (collisions != null) {
                //System.out.println(collisions.size()+" collisions at "+simTime);
                for (int i = 0; i < vars.length; i++)  // back up in time
                    vars[i] = old_vars[i];  // revert vars
                if (h <= this.TOL) {  // If close enough in time to the collision.
                    collisionCount++;  // Come through here many times in a row = stuck!
                    // apply the collision (even though it is 'from the near future')
                    handleCollisions(collisions);
                    h = timeStep - timeAdvanced;  // try to step to end of time period timeStep
                    if (collisionCount < 10)
                        continue;  // try to advance again
                    else {
                        System.out.println("stuck in advance(): many collisions in a row");
                        break;  // we are stuck!  break out of the loop
                    }
                }
            } else {  // no collision, we have advanced by time h
                collisionCount = 0;  // if we advanced, we are not stuck!
                timeAdvanced += h;
                simTime += h;
                if (h <= this.TOL)
                    continue;  // don't subdivide timeperiod once its very small
            }
            h = h / 2;  // try to advance again by a smaller time
        }
        //if (collisionCount > 0)
        //  System.out.println("collision with "+collisionCount+" iterations at "+simTime);
        lastTimeStep = timeAdvanced;
    }

    // returns a vector with list of collisions, or null if no collisions.
    public abstract Vector findAllCollisions();

    // Adjust simulation for the given collisions.
    // For example, reverse velocities of objects colliding against a wall.
    public abstract void handleCollisions(Vector collisions);

}
