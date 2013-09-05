package core;

import static org.junit.Assert.*;

import org.jblas.DoubleMatrix;
import org.junit.Before;
import org.junit.Test;

import domain.Execution;
import domain.Phase;
import domain.Session;
import domain.network.FakeNode;
import domain.network.Node;

public class SessionTest {

	Node localNode;
	Session session;

	double[] impulseResponses = { 0.0122, 0.0068, 0.0686, 0.1061, 0.0763,
			-0.0071, -0.0985, -0.1537, -0.1534, -0.1074, -0.0426, 0.0131,
			0.0439, 0.0488, 0.0373, 0.0217, 0.0103, 0.0055, 0.0050, 0.0051,
			0.0033, -0.0007 };

	double[][] expectedMatrixA = {

			{ 0.8216387101399824, -0.4968986018097526, -0.10307764582740445,
					-0.0038718697884600577, -0.0023294699538915354,
					-1.9724459372758816E-4, -3.267636269546181E-5,
					2.5648778471495254E-4, -1.8363828674442776E-4,
					-6.571688683938801E-5, 2.6412689195783197E-5 },
			{ 0.4968976683986355, 0.7770613240226641, -0.1688004704144454,
					0.2059518247363174, -0.0022257809932847636,
					-4.442525941294531E-4, 2.0987878434335343E-5,
					3.565983235325936E-4, 6.282000261760494E-4,
					-5.650252933786578E-4, 1.4041251948703246E-4 },
			{ 0.10307852509438245, -0.168808307686376, 0.7788395703293963,
					0.450104755855965, -1.0131859468495057E-4,
					1.2158612503467762E-4, -3.095123541637713E-5,
					-2.817315426603456E-5, -3.6996771174662476E-4,
					2.2808365962516733E-4, -5.512215715575613E-5 },
			{ 0.0038742603774503115, -0.2059740047707813, -0.45006379012592057,
					0.37994294545902996, 0.03850764340294818,
					0.0018460814379202112, 8.416404727687246E-4,
					-0.003668346017118882, 0.007365132634122893,
					-0.0016312300770859258, 1.9402414801539844E-4 },
			{ -0.0024445973172223523, 0.002774171245353063,
					5.513914573166967E-4, 0.03436744782050827,
					-0.02596179116387584, -0.44655489501077106,
					0.18118382034337588, -0.2648694036662246,
					0.09927634074843482, -0.1630230450676072,
					0.10750629565633527 },
			{ 2.842848893568478E-4, -4.030153291181282E-4,
					-6.480822642163653E-4, 1.1658578429685207E-4,
					0.39033145147395193, -0.617804168091744,
					-0.09169021409141737, 0.4545627169842995,
					-0.3829600974050032, 0.07142247050449155,
					-0.031423376198607515 },
			{ 1.7774310054167325E-4, 3.7519842896593936E-4,
					-0.0021090148020957294, 0.004827014391035944,
					0.008694051958294771, 0.06404210741899574,
					-0.3735412878779704, -0.6219847387983777,
					-0.5169034664645974, 0.13468674334689346,
					-0.0011054467354070733 },
			{ -1.8569015751301876E-4, 0.0013766026538490905,
					-0.00158345394563697, 0.002738297838304149,
					0.09746713594777112, 0.3625754068534086,
					0.8218749155059852, 0.05355118750608526,
					-0.41749808842634034, -0.0010567806177222971,
					0.08881988486950886 },
			{ -3.787795430239549E-4, -5.648038990111459E-4,
					0.0019487286047066288, 0.0025657534655949554,
					0.20610486666055666, 0.3684272628347598,
					-0.4197843395377061, 0.19944495194040218,
					-0.29649115128673, -0.49323806627464695,
					0.19844717850807453 },
			{ 4.042093889422005E-4, -6.173137407179397E-4,
					-0.0025647717418157967, 0.009808522383803808,
					-0.029396087459775427, 0.09125264671654684,
					-0.2901472888784973, 0.37280390033328153,
					0.4811796279233361, -0.2289295315974148,
					0.35141657948883304 },
			{ 7.792065309675245E-5, 7.039948038967125E-4,
					-0.0011727841462543243, -7.44664517289273E-4,
					-0.026904846087561496, -0.04504176779558322,
					0.16738728149611443, -0.09658033314506323,
					0.01841492030464989, -0.6605181251852452,
					-0.7692134536834554 } };

	double[] expectedEigenvals = { 0.9328880467738535, 0.871627869420588,
			/*0.8463250407483807, 0.825305912999191, 0.46190743101925374,
			1.2048044030847112E-16*/};

	@Before
	public void setUp() throws Exception {
		localNode = new FakeNode();
	}

	@Test
	public void addAllExecutions() {
		session = new Session(localNode, 5, 100);
		for (int i = 1; i <= 5; i++) {
			/*
			 * We initially defined 5 executions, so all must return true
			 */
			assertNotNull(session.createNewExecution());
		}
		/*
		 * The addition of a sixth execution must return false, since we only
		 * defined 5
		 */
		assertNull(session.createNewExecution());
		// Verify that we have 5 executions
		assertEquals(5, session.getCurrentNumberOfExecutions());
	}

	@Test
	public void checkIfNewExecutionIsExpected() {
		session = new Session(localNode, 5, 100);
		session.createNewExecution();
		session.getInitExecution().setRound(19);
		assertFalse(session.newExecutionExpected());
		session.getInitExecution().setRound(20);
		assertTrue(session.newExecutionExpected());
		session.getInitExecution().setRound(99);
		assertFalse(session.newExecutionExpected());
	}

	@Test
	public void createNewUnnumberedExecution() {
		session = new Session(localNode, 4, 100);
		assertEquals(1, session.createNewExecution().getExecutionNumber());
		assertEquals(2, session.createNewExecution().getExecutionNumber());
	}

	@Test
	public void createNewNumberedExecution() {
		session = new Session(localNode, 4, 100);
		assertEquals(2, session.createNewExecution(2).getExecutionNumber());
		assertEquals(3, session.createNewExecution(3).getExecutionNumber());
		assertEquals(2, session.getCurrentNumberOfExecutions());
	}

	@Test
	public void createMoreThanDefinedExecutions() {
		session = new Session(localNode, 3, 100);
		assertNotNull(session.createNewExecution());
		session.createNewExecution();
		session.createNewExecution();
		assertNull(session.createNewExecution());
	}

	@Test
	public void sessionTerminatesSuccessfully() {
		session = new Session(localNode, 3, impulseResponses.length);
		Execution e1 = session.createNewExecution();
		Execution e2 = session.createNewExecution();
		Execution e3 = session.createNewExecution();

		// No execution should be completed
		assertEquals(0, session.getCompletedExecutions());
		assertFalse(session.hasTerminated());

		// We make two of the executions the same. The result must be the
		// expected eigenvalues
		for (int i = 2; i <= impulseResponses.length; i++) {
			e1.addValToRound(impulseResponses[i - 1], i);
			e2.addValToRound(impulseResponses[i - 1], i);
			e3.addValToRound(0.05, i);
		}

		for (int i = 2; i <= impulseResponses.length + 1; i++) {
			e1.setRound(i);
			e2.setRound(i);
			e3.setRound(i);
		}
		// Must do this, otherwise the first value will always be 0 or 1
		// We need it to be the test impulse response
		e1.setImpulseResponse(1, impulseResponses[0]);
		e2.setImpulseResponse(1, impulseResponses[0]);
		e3.setImpulseResponse(1, impulseResponses[0]);

		e1.computeRealizationMatrix();
		e1.setPhase(Phase.GOSSIP);
		e1.computeMedianEigenvalues();
		e1.setPhase(Phase.TERMINATED);
		session.addCompletedExecution();
		e2.computeRealizationMatrix();
		e2.setPhase(Phase.GOSSIP);
		e2.computeMedianEigenvalues();
		e2.setPhase(Phase.TERMINATED);
		session.addCompletedExecution();

		e3.computeRealizationMatrix();
		e3.setPhase(Phase.GOSSIP);
		DoubleMatrix dd = new DoubleMatrix(e3.computeMedianEigenvalues());
		dd.print();
		e3.setPhase(Phase.TERMINATED);
		session.addCompletedExecution();

		assertEquals(3, session.getCompletedExecutions());
		assertArrayEquals(expectedEigenvals, session.getComputedEigenvalues(),
				0.05);
		assertTrue(session.hasTerminated());
	}
}
