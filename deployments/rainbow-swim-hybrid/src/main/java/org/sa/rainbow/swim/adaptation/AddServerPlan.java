package org.sa.rainbow.swim.adaptation;

import org.sa.rainbow.core.Rainbow;
import org.sa.rainbow.core.adaptation.IAdaptationExecutor;
import org.sa.rainbow.core.ports.IModelsManagerPort;
import org.sa.rainbow.core.ports.IModelDSBusPublisherPort.OperationResult;
import org.sa.rainbow.core.ports.IModelDSBusPublisherPort.Result;
import org.sa.rainbow.model.acme.AcmeModelInstance;
import org.sa.rainbow.model.acme.swim.commands.AddServerWithTypeCmd;
import org.sa.rainbow.model.acme.swim.commands.SwimCommandFactory;

public class AddServerPlan extends SwimExtendedPlan {

	private String			   m_server;
	private boolean            m_outcome;
	private IModelsManagerPort m_modelsManager;
	
	//private SwimModelUpdateOperatorsImpl 			   m_model;
	private AcmeModelInstance m_reference;	
	
	// server is either 1, 2, or 3
	public AddServerPlan (AcmeModelInstance m, String server) {
		m_reference = m;
		m_server = server;
	}
	
  @Override
  public Object evaluate(Object[] argsIn) {
    IAdaptationExecutor<SwimExtendedPlan> executor = 
        Rainbow.instance().getRainbowMaster().strategyExecutor
        (m_reference.getModelName() + ":" + m_reference.getModelType());
    SwimCommandFactory cf = (SwimCommandFactory) m_reference.getCommandFactory();
    AddServerWithTypeCmd cmd = cf.addServerWithTypeCmd(m_reference.getModelInstance().getComponent("LB0"), m_server);
    System.out.println ("Changing Instructions inside AddServerPlan");
    if (executor == null) {
      System.out.println("executor null"); return false;
    }
    else {
    OperationResult result = executor.getOperationPublishingPort().publishOperation(cmd);
    m_outcome = result.result == Result.SUCCESS;
    System.out.println ("Done: " + m_outcome);
    return m_outcome; }
  }

	@Override
	public boolean getOutcome() {
		// TODO Auto-generated method stub
		return m_outcome;
	}

}
