package eclipse2026;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;

public class Agente extends Agent
{
    private static final long serialVersionUID = 1L;
    protected CyclicBehaviour cyclicBehaviour;

    //hay que definir el método setup para establecer el comportamiento del agente
    public void setup()
    {
        cyclicBehaviour = new CyclicBehaviour(this) {
            private static final long serialVersionUID = 1L;

            //método abstracto que hay que implementar se ejecuta ciclicamente
            public void action()
            {
                block();
            }
        };

        addBehaviour(cyclicBehaviour);
    }
}
