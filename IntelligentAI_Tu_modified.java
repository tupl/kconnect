import connectK.CKPlayer;
import connectK.BoardModel;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IntelligentAI extends CKPlayer
{
    // Declaring Variables
	public static final int MAX_DEPTH = 10;
    public static final int TIME_OFFSET = 600;
    public static final boolean DEBUG_MODE = true;
	public static final Integer LOWER_BOUND = Integer.MIN_VALUE;
	public static final Integer UPPER_BOUND = Integer.MAX_VALUE;
    public static final Integer MAX_SCORE = 1000;
	
	private int depthLimit = 1;
	private int KAchieve = 0;
	private int K_1Block = -1;
	private int K_1FreeTwoSide = -2; // Free two side
	private int K_1FreeOneSide = -3; // Free one side
	private int K_2FreeTwoUnoccupied = -5; // At least two space both or left or right
	private int K_2FreeOne = -6; // XXX_X or X_XXX
		
	private Point lastPoint = new Point();
   
	private long startTime;
	private int myDeadLine, otherPlayer;

    /* Default Code from DummyAI */
	public IntelligentAI(byte player, BoardModel state)
    {
		super(player, state);
		teamName = "IntelligentAI";
        otherPlayer = (player == 1) ? 2 : 1;
	}

	@Override
	public Point getMove(BoardModel state)
    {
		try
		{
            // DEBUG ONLY
            if (DEBUG_MODE)
    			System.out.println("Start thinking");

            int currentPlayer = player;

			for (int i = 1; i < MAX_DEPTH; i++)
            {
				depthLimit = i + 1;
				Point newAction = getAction(state, currentPlayer);

                // DEBUG ONLY
                if (DEBUG_MODE)
    				System.out.println("Depth limit " + Integer.toString(depthLimit) +  " with action = " +
	    								lastPoint.toString());

				// If we do not stop due to out of time, we should save this result
				if (!outOfTime())
                {
                    // DEBUG ONLY
                    if (DEBUG_MODE)
    					System.out.println("An action is saved.");

					lastPoint = newAction;
				}
				else
                {
                    // DEBUG ONLY
                    if (DEBUG_MODE)
                        System.out.println("Ran out of time. RETURN IMMEDIATELY!");

					return lastPoint;
                }

                // Change currentPlayer on next level
                currentPlayer = (currentPlayer == player) ? otherPlayer : player;
			}
			
            // DEBUG ONLY
            if (DEBUG_MODE)
    			System.out.println("Done thinking");
		}
		catch (Exception exception)
        {
            // DEBUG ONLY
            if (DEBUG_MODE)
            {
    			System.out.println("Some error in getMove(BoardModel state) with lastPoint = " + lastPoint);
	    		System.out.println(exception);
            }
		}

		return lastPoint;
	}

	@Override
	public Point getMove(BoardModel state, int deadline) 
    {
		// Set the start time and deadline
		myDeadLine = deadline;
		startTime = System.currentTimeMillis();
		return getMove(state);
    }

    ///////////////////////////////////////////////////////////////

    /* Private Helper Functions */
	// This function will print a list of children starting from a node
	private Point getAction(BoardModel state, int currentPlayer)
    {
		// This always is the max node, my player;
		// Get the current player turn
		
		// If we are out of time, stop (Base Case)
		if (outOfTime())
            return new Point(0, 0);
		
        // If the current state is at the beginning (all empty)
		if (state.spacesLeft == state.getHeight() * state.getWidth())
        {
            // we will choose the middle cell
			if (state.gravityEnabled())
				return new Point(state.getWidth() / 2, 0);
			return new Point(state.getWidth() / 2, state.getHeight() / 2);
		}

		Integer alpha = LOWER_BOUND;
		Integer beta = UPPER_BOUND;	
		Integer v = LOWER_BOUND;
	
		Point action = new Point(0, 0);

		// true value of isMax to help order the children by their evaluations
		List<BoardModel> children = getChildren(state, currentPlayer, true);

		for (int i = 0; i < children.size(); i++)
        {
            // Base Case
			if (v >= beta)
				break;

			BoardModel child = children.get(i);	
			Integer result =  alphaBeta(child, false, alpha, beta, 2);
			if (result > v)
            {
				v = result;
				action = child.getLastMove();
			}

			if (v > alpha)
                alpha = v;
		}

        if (DEBUG_MODE)
        {
    		System.out.println("getAction return = " + action.toString());
            this.printChildren(children);
        }
		
		return action;
	}	

    // This function will do alpha-beta pruning
	private Integer alphaBeta(BoardModel state, boolean isMax,
                                Integer alpha, Integer beta, int depth)
    {	
		// If we are out of time, stop right away
		if (outOfTime())
            return 0;
		
        // check if we win this state or not
		byte winner = state.winner();
		if (winner == 1 || winner == 2)     
            return (winner == player) ? MAX_SCORE : -MAX_SCORE;
	
		// At the depth limit
		if (depth >= depthLimit)
			return staticEval(state);

		// Get the current player turn
		int currentPlayer = currentTurn( state );
		
		// This is max node
		if ( isMax ) {
			List<BoardModel> children = getChildren( state, currentPlayer, true );
			Integer v = LOWER_BOUND;
			for( int i = 0; i < children.size(); ++i ) {
				BoardModel child = children.get(i);
				Integer result =  alphaBeta( child, false, alpha, beta, depth + 1 );
				if ( result > v ) {
					v = result;
				}
				if ( v >= beta ) {
					return v;
				}
				if ( v > alpha ) {
					alpha = v; 
				}
			}
			return v;
		}
		else
		{
			// This is min node
			List<BoardModel> children = getChildren( state, currentPlayer, false );
			Integer v = UPPER_BOUND;
			for( int i = 0; i < children.size(); ++i ) {
				BoardModel child = children.get(i);
				Integer result =  alphaBeta( child, true, alpha, beta, depth + 1 );
				if ( result < v ) {
					v = result;
				}
				if ( v <= alpha ) {
					return v;
				}
				if ( v < beta ) {
					beta = v;
				}
			}
			return v;
		}
	}

    // This function will return a list of children boards from the given state
	private List<BoardModel> getChildren(BoardModel state, int current, boolean isMax)
    {
		if (state.gravityEnabled())
			return getChildrenWithGravity(state, current, isMax);

        // offset 8 directions around the piece
		int numOffset = 8;
		int[] xOffset = {0, 1, 1, 1, 0, -1, -1, -1};
		int[] yOffset = {1, 1, 0, -1, -1, -1, 0, 1};

        // init list board of children + set for eleminating duplicates
		List<BoardModel> children = new ArrayList<BoardModel>();				
		Set<String> set = new HashSet<String>();
		
        for (int i = 0; i < state.getWidth(); i++)
        {
            for (int j = 0; j < state.getHeight(); j++)
            {
                if (state.getSpace(i, j) == 0)
                {
                    // Check if surrounding has occupied
                    for (int k = 0; k < numOffset; k++)
                    {
                        int nextX = i + xOffset[k];
                        int nextY = j + yOffset[k];

                        if (validPoint(state, nextX, nextY))
                        {
                            // If the arround has at least one occupied 
                            if (state.getSpace(nextX, nextY) != 0)
                            {    
                                byte value = 0;
                                if (current == 1)
                                    value = 1;
                                else if (current == 2)
                                    value = 2;
                                
                                BoardModel newState = state.placePiece(new Point(i, j), value);
                                String stateStr = newState.toString();
                                if (!set.contains(stateStr))
                                {
                                    set.add(stateStr);
                                    children.add(newState);
                                }
                            }
                        }
                    }
                }
            }
        }

		return children;
	}
	
	// This children function deal with gravity on
	private List<BoardModel> getChildrenWithGravity(BoardModel state, int current, boolean isMax)
    {	
		List<BoardModel> children = new ArrayList<BoardModel>();
		
		for (int i = 0; i < state.getWidth(); i++)
        {
			for (int j = 0; j < state.getHeight(); j++)
            {
				if (state.getSpace(i, j) == 0)
                {
					int newX = i;
					int newY = j - 1;

					if ((!validPoint(state, newX, newY)) || (state.getSpace(newX, newY) != 0))
                    {
						byte value = 0;
						if (current == 1)
							value = 1;
						else if (current == 2)
							value = 2;

						BoardModel newState = state.placePiece(new Point(i, j), value);
						children.add(newState);
					}
				}
			}
		}

		return children;
	}
	
    // This function will 
	private Integer staticEval(BoardModel state)
    {
		Integer score = 0;
		
		Integer winMoveWeight = MAX_SCORE;
		Integer winMoveValue = winMove(state);
		score +=  winMoveWeight * winMoveValue;
		
		Integer numberConnects = numberOfConnectLine( state );
		Integer numberConnectsWeight = 1;
		score += numberConnectsWeight * numberConnects;
		
		score += 2 * noname( state );
				
		Integer centerWeight = 1;
		Integer centerValue = piecesNearCenter( state );
		score += centerWeight * centerValue;
		
		return score;
	}	
    // This function will check whether the Point is valid or not in the given BoardModel
	private boolean validPoint(BoardModel state, Point pt)
    {
		int x = (int) pt.getX();
		int y = (int) pt.getY();
		int height = state.getHeight();
		int width = state.getWidth();

		if (x < 0 || x >= width)
			return false;
		else if (y < 0 || y >= height)
			return false;
        else
    		return true;		
	}
	
    // This function will check whether x & y coordinates are valid or not in the given BoardModel
	private boolean validPoint(BoardModel state, int x, int y)
    {
		int height = state.getHeight();
		int width = state.getWidth();

		if (x < 0 || x >= width)
			return false;
		else if (y < 0 || y >= height)
			return false;
		else
    		return true;
	}
	
    // This function will print out all the children in the List of BoardModel (DEBUG ONLY)
	private void printChildren(List<BoardModel> children)
    {
        if (DEBUG_MODE)
        {
		    for (int i = 0; i < children.size(); i++)
            {
	    		BoardModel child = children.get(i);
	    
		    	System.out.println("My score : " + staticEval(child));
			    System.out.println(child.toString());
    			System.out.println(child.getLastMove());
	    	}
        }
	}
	
	// This function will help counting the number of connected lines
	private Integer numberOfConnectLine(BoardModel state)
    {
		Integer meCount = 0;
		Integer oppoCount = 0;
		
		int numOffset = 8;
		int[] xOffset = {-1, 0, 1, 1,  1,  0, -1, -1};
		int[] yOffset = { 1, 1, 1, 0, -1, -1, -1,  0};
		
		for (int i = 0; i < state.getWidth(); i++)
        {
			for (int j = 0; j < state.getHeight(); j++)
            {
				int current = state.getSpace(i, j);
				if (current != 0)
                {
					for( int k = 0; k < numOffset; ++k ) {
						int nextX = i + xOffset[k];
						int nextY = j + yOffset[k];
						if (validPoint(state, nextX, nextY) && state.getSpace(nextX, nextY) == current)
                        {
							if (current == player)
								meCount++;
							else
								oppoCount++;
						}
					}
				}
			}
		}
		
		return meCount - oppoCount;
	}

    // This function will check whether the give board state is the winning state or not
	private Integer winMove(BoardModel state)
    {
		int value = state.winner();

		if (value == player)
			return 1;
		else if (value == otherPlayer)
			return -1;
	    else
    		return 0;
	}
	
	// This function will help favor the one at the center than at the boundary
	private Integer piecesNearCenter(BoardModel state)
    {
		Integer meScore = 0;
		Integer oppoScore = 0;
		int otherPlayer = (player == 1) ? 2 : 1;
		
		Integer centerX = state.getWidth() / 2;
		Integer centerY = state.getHeight() / 2;
		
		for (int i = 0; i < state.getWidth(); i++)
        {
			for (int j = 0; j < state.getHeight(); j++)
            {
				int value = state.getSpace(i, j);
				if (value == player)
					meScore += Math.abs(i - centerX) + Math.abs(j - centerY);
				else if (value == otherPlayer)
					oppoScore += Math.abs(i - centerX) + Math.abs(j - centerY);
			}
		}
		
		return oppoScore - meScore;
	}

	// Detect if this horizontal is winning path with assumption is that
	// Direction
    //   0   1  2
    //      f1  3
	private Integer getFeature(BoardModel state, Point f, int dire, int current)
    {	
		int oppoCurrent = (current == 1) ? 2 : 1;
		
		// Check the head
		int[] xOffset = {-1,  0, +1, +1};
		int[] yOffset = {+1, +1, +1,  0};
		int K = state.getkLength();
		
		// Find f1, f2
		int length = 1; // assum that length is 1
		Point f1 = new Point(f);
		Point f2 = new Point(f);
		
		//Going in downward direction
		int i = 1;
		while (true)
        {
			int newX = (int) f1.getX() - i * xOffset[dire];
			int newY = (int) f1.getY() - i * yOffset[dire];
			Point newF1 = new Point( newX, newY );
			if ( validPoint( state, newF1 ) && state.getSpace( newX, newY ) == current ) {
				++i;
				++length;
			}
			else {
				break;
			}
		}

		f1 = new Point( (int) f1.getX() - (i - 1) * xOffset[dire],
						(int) f1.getY() - (i - 1) * yOffset[dire] );
		
		//Going in upward direction
		i = 1;
		while( true ) {
			int newX = (int) f2.getX() + i * xOffset[dire];
			int newY = (int) f2.getY() + i * yOffset[dire];
			Point newF2 = new Point( newX, newY );
			if ( validPoint( state, newF2 ) && state.getSpace( newX, newY ) == current ) {
				++i;
				++length;
			}
			else {
				break;
			}
		}
		f2 = new Point( (int) f2.getX() + (i - 1) * xOffset[dire],
				(int) f2.getY() + (i - 1) * yOffset[dire] );
		
		if ( length >= K ) {
			return KAchieve;
		}else if ( length == K-1 ) {
			boolean headBlock = false;
			boolean tailBlock = false;
			int headX = (int) f1.getX() - xOffset[dire];
			int headY = (int) f1.getY() - yOffset[dire];
			int tailX = (int) f2.getX() + xOffset[dire];
			int tailY = (int) f2.getY() + yOffset[dire];
			
			if ( !validPoint( state, headX, headY) || 
				  state.getSpace( headX, headY ) == oppoCurrent ) {
				headBlock = true;
			}

			if ( !validPoint( state, tailX, tailY) || 
				  state.getSpace( tailX, tailY ) == oppoCurrent ) {
				tailBlock = true;
			}
			
			if ( headBlock && tailBlock ) {
				return K_1Block;
			}
			else if ( !headBlock && !tailBlock ) {
				return K_1FreeTwoSide;
			}
			else {
				return K_1FreeOneSide;
			}
		}else if ( length == K-2 ) {

			boolean headBlock = false;
			boolean tailBlock = false;
			int headX = (int) f1.getX() - xOffset[dire];
			int headY = (int) f1.getY() - yOffset[dire];
			int head2X = (int) f1.getX() - 2 * xOffset[dire];
			int head2Y = (int) f2.getX() - 2 * yOffset[dire];
			int tailX = (int) f2.getX() + xOffset[dire];
			int tailY = (int) f2.getY() + yOffset[dire];
			int tail2X =  (int) f1.getX() + 2 * xOffset[dire];
			int tail2Y =  (int) f2.getX() + 2 * yOffset[dire];
			
			if ( validPoint( state, headX, headY ) &&
				 validPoint( state, head2X, head2Y ) &&
				 state.getSpace( headX, headY ) == 0 &&
				 state.getSpace( head2X, head2Y ) == current) {
				return K_2FreeOne;
			}
			else if ( validPoint( state, tailX, tailY ) &&
					 validPoint( state, tail2X, tail2Y ) &&
					 state.getSpace( tailX, tailY ) == 0 &&
					 state.getSpace( tail2X, tail2Y ) == current) {
				return K_2FreeOne;
			}
			else
			{
				if ( !validPoint( state, headX, headY) || 
					  state.getSpace( headX, headY ) == oppoCurrent ) {
					headBlock = true;
				}

				if ( !validPoint( state, tailX, tailY) || 
					  state.getSpace( tailX, tailY ) == oppoCurrent ) {
					tailBlock = true;
				}
				
				if ( !headBlock && !tailBlock ) {
					return K_2FreeTwoUnoccupied;
				}
			}
		}
		return length;
	}

	/* ======================================================= */
	
	boolean goForIt( BoardModel state, int x, int y, int dire) {
		int[] xOffset = {-1,  0, +1, +1};
		int[] yOffset = {+1, +1, +1,  0};
		
		int newX = x + xOffset[dire];
		int newY = y + yOffset[dire];
		
		if ( !validPoint( state, newX, newY ) ) {
			return true;
		}else{
			if ( state.getSpace(newX, newY) != state.getSpace(x, y) )
				return true;
		}
		
		return false; 
	}
	
	Integer noname( BoardModel state ) {
		Integer meScore = 0;
		Integer oppoScore = 0;
		for ( int i = 0; i < state.getWidth(); ++i ) {
			for ( int j = 0; j < state.getHeight(); ++j ) {
				int current = state.getSpace( i, j );
				if ( current != 0 ) {
					for ( int dire = 0; dire < 4; ++dire ) {
						if ( goForIt( state, i, j, dire) ) {
							Integer f =  getFeature( state, new Point(i, j), dire, current );
							Integer value = 0;
							if ( f > 0 ) {
								// Only about length
								value = f * 15;
							}
							else{
								// feature
//								int KAchieve = 0;
//								int K_1Block = -1;
//								int K_1FreeTwoSide = -2; // Free two side
//								int K_1FreeOneSide = -3; // Free one side
//								int K_2FreeTwoUnoccupied = -5; // At least two space both or left or right
//								int K_2FreeOne = -6; // XXX_X or X_XXX
								if ( f == K_1Block ) {
									value = 30;
								} else if ( f == K_1FreeOneSide ) {
									value = 80;
								} else if ( f == K_1FreeTwoSide ) {
									value = 150;
								} else if ( f == K_2FreeTwoUnoccupied ) {
									value = 60;
								} else if ( f == K_2FreeOne ) {
									value = 20;
								} else if ( f == KAchieve ) {
									value = MAX_SCORE;
								}
							}
							
							if ( current == player ) {
								meScore += value;
							}else{
								oppoScore += value;
							}
							
						}
					}
				}
			}
		}
		return meScore - oppoScore;
	}	
	
	// This function will check if it is out of time
	private boolean outOfTime()
    {
		if ((System.currentTimeMillis() - startTime) > (myDeadLine - TIME_OFFSET))
			return true;
		else
    		return false;
	}	
}
