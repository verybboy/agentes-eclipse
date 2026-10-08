
package practica2;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.ThreadedBehaviourFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.swing.JOptionPane;

import jade.content.lang.sl.SLCodec;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.*;

public class Agente extends Agent {

    private static final long serialVersionUID = 1L;

    protected ParallelBehaviour compoundBehaviour;
    protected CyclicBehaviourEnviar messageSender = null;
    protected CyclicBehaviourImprimir messageReceiver = null;

    private ThreadedBehaviourFactory thrBFSender =
            new ThreadedBehaviourFactory();

    private ThreadedBehaviourFactory thrBFReceiver =
            new ThreadedBehaviourFactory();

    @Override
    protected void setup() {

        System.out.println("Soy el agente " + getLocalName());

        // Registrar el servicio de mensajería en el DF
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());

        ServiceDescription sd = new ServiceDescription();
        sd.setName("ServicioMensajeria");
        sd.setType("Mensajeria");
        sd.addOntologies("Ontologia");
        sd.addLanguages(new SLCodec().getName());

        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            System.err.println(" -Agente " + getLocalName()
                    + ": " + e.getMessage());
        }

        // Crear el comportamiento compuesto
        compoundBehaviour = new ParallelBehaviour(
                this, ParallelBehaviour.WHEN_ALL);

        // Crear los dos comportamientos cíclicos
        messageSender = new CyclicBehaviourEnviar(this);
        messageReceiver = new CyclicBehaviourImprimir(this);

        // Añadirlos al compuesto en hilos independientes
        compoundBehaviour.addSubBehaviour(
                thrBFSender.wrap(messageSender));

        compoundBehaviour.addSubBehaviour(
                thrBFReceiver.wrap(messageReceiver));

        // Añadir el comportamiento compuesto al agente
        addBehaviour(compoundBehaviour);
    }

    @Override
    protected void takeDown() {

        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        System.out.println(" --- Agente "
                + getAID().getName() + " finalizando ---");
    }
}


// COMPORTAMIENTO PARA ENVIAR MENSAJES

class CyclicBehaviourEnviar extends CyclicBehaviour {

    private static final long serialVersionUID = 1L;

    public CyclicBehaviourEnviar(Agent agent) {
        super(agent);
    }

    @Override
    public void action() {

        AID[] arrAgentIds = null;
        String userMsg = null;

        System.out.println(" - Introduce un mensaje "
                + "para enviarlo a otros agentes: ");

        BufferedReader buffer = new BufferedReader(
                new InputStreamReader(System.in));

        try {
            userMsg = buffer.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // Si se cierra la entrada, finalizar el agente
        if (userMsg == null) {
            myAgent.doDelete();
            return;
        }

        // Buscar agentes que ofrecen el servicio
        arrAgentIds = searchServiceAgents("Mensajeria");

        if (arrAgentIds != null && arrAgentIds.length > 0) {

            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.setSender(myAgent.getAID());

            for (int i = 0; i < arrAgentIds.length; i++) {
                msg.addReceiver(arrAgentIds[i]);
            }

            msg.setContent(userMsg);
            msg.setOntology("Ontologia");
            msg.setLanguage(new SLCodec().getName());

            myAgent.send(msg);

            System.out.println("Mensaje enviado correctamente.");
        }
    }

    private AID[] searchServiceAgents(String msgType) {

        AID[] agentIds = null;

        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription templateSd = new ServiceDescription();

        templateSd.setType(msgType);
        template.addServices(templateSd);

        try {

            DFAgentDescription[] arrResult =
                    DFService.search(myAgent, template);

            if (arrResult.length > 0) {

                System.out.println(" -Agente "
                        + myAgent.getLocalName()
                        + " ha encontrado los siguientes agentes:");

                agentIds = new AID[arrResult.length];

                for (int i = 0; i < arrResult.length; i++) {

                    agentIds[i] = arrResult[i].getName();

                    System.out.println(" -Agente "
                            + agentIds[i].getLocalName()
                            + ": Tipo de servicio -> Mensajeria.");
                }

            } else {

                JOptionPane.showMessageDialog(
                        null,
                        "Agente " + myAgent.getLocalName()
                        + " no encontró ningún servicio",
                        "Información",
                        JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (FIPAException e) {

            JOptionPane.showMessageDialog(
                    null,
                    "Agente " + myAgent.getLocalName()
                    + ": " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);

            e.printStackTrace();
        }

        return agentIds;
    }
}


// COMPORTAMIENTO PARA RECIBIR MENSAJES

class CyclicBehaviourImprimir extends CyclicBehaviour {

    private static final long serialVersionUID = 1L;

    public CyclicBehaviourImprimir(Agent agent) {
        super(agent);
    }

    @Override
    public void action() {

        MessageTemplate mt =
                MessageTemplate.MatchPerformative(
                        ACLMessage.INFORM);

        ACLMessage msg = myAgent.blockingReceive(mt);

        if (msg != null) {

            String strContent = msg.getContent();

            System.out.println("Agente "
                    + myAgent.getLocalName()
                    + " recibió un mensaje del agente "
                    + msg.getSender().getLocalName() + ": ");

            System.out.println(" -Mensaje: " + strContent);
        }
    }
}
