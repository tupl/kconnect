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

	private int depthLimit = 1;
    private int myPlayer = -1;
	private int otherPlayer = -1;
	private int KAchieve = 0;
	private int K_1Block = -1;
	private int K_1FreeTwoSide = -2; // Free two side
	private int K_1FreeOneSide = -3; // Free one side
	private int K_2FreeTwoUnoccupied = -5; // At least two space both or left or right
	private int K_2FreeOne = -6; // XXX_X or X_XXX
		
	private Integer lower = Integer.MIN_VALUE;
	private Integer upper = Integer.MAX_VALUE;
	private Integer scoreLoseWin = 100000000;

	private Point lastPoint = new Point();
   
	private long startTime;
	private int myDeadLine;

    /* Default Code from DummyAI */
	public IntelligentAI(byte player, BoardModel state)
    {
		super(player, state);
		teamName = "IntelligentAI";
	}

	@Override
	public Point getMove(BoardModel state)
    {
		try
		{
            // DEBUG ONLY
			System.out.println("Start thinking");

			for (int i = 1; i < MAX_DEPTH; i++)
            {
				depthLimit = i + 1;
				Point newAction = getAction(state);

                // DEBUG ONLY
				System.out.println("Depth limit " + Integer.toString(depthLimit) +  " with action : " +
									lastPoint.toString());

				// If we do not stop due to out of time, we should save this result
				if (!outOfTime())
                {
                    // DEBUG ONLY
					System.out.println("An action is saved.");

					lastPoint = newAction;
				}
				else
					return lastPoint;	
			}
			
			System.out.println( "Done thinking");
		}
		catch( Exception e ) {
			System.out.println( "Some error in getMove(BoardModel state)" );
			System.out.println( lastPoint );
			System.out.println(e);
		}
		return lastPoint;
	}

	@Override
	public Point getMove(BoardModel state, int deadline) 
    {
		// Set the start time and deadline
		myDeadLine = deadline;
		startTime = System.currentTimeMillis();
		return getMove( state );
    }

    ///////////////////////////////////////////////////////////////

    /* Private Helper Functions */
	// This function will return the current player's turn
	private byte currentTurn(BoardModel state)
    {
		int player1 = 0;
		int player2 = 0;
		for (int i = 0; i < state.getWidth(); i++)
        {
			for (int j = 0; j < state.getHeight(); j++)
            {
				if (state.getSpace(i, j) == 1)
					player1++;
				else if (state.getSpace(i, j) == 2)
					player2++;
			}
		}
		
		if (player1 == player2)
			// player 1 go first
			return 1;
		else
			// now player 2's turn
			return 2;
	}
	
    // This function 
	private boolean validPoint(BoardModel state, Point pt)
    {
		int x = (int) pt.getX();
		int y = (int) pt.getY();
		int height = state.getHeight();
		int width = state.getWidth();
		if ( x < 0 || x >= width ) {
			return false;
		}
		if ( y < 0 || y >= height ) {
			return false;
		}		
		return true;		
	}
	
	boolean validPoint( BoardModel state, int x, int y ) {
		int height = state.getHeight();
		int width = state.getWidth();
		if ( x < 0 || x >= width ) {
			return false;
		}
		if ( y < 0 || y >= height ) {
			return false;
		}		
		return true;
	}
	
	void printChildren( List<BoardModel> children ) {
		for( int i = 0; i < children.size(); ++i ) {
			BoardModel child = children.get(i);
			
			System.out.println( "My score : " + staticEval( child ) );
			System.out.println( child.toString() );
			System.out.println( child.getLastMove() );
		}
	}

	void printMap( BoardModel state ) {
		String rs = state.toString();
		
		for (int i = 0; i < rs.length(); ++i) {
			if ( rs.charAt(i) == '0' ) System.out.print(" ");
			if ( rs.charAt(i) == '1' ) System.out.print("x");
			if ( rs.charAt(i) == '2' ) System.out.print("o");
			if ( rs.charAt(i) == '\n' ) System.out.print("\n");
		}
	}
	
	
	/*
	 * This one helps counting the number of connected lines
	 */
	Integer numberOfConnectLine( BoardModel state ) {
		Integer meCount = 0;
		Integer oppoCount = 0;
		
		int numOffset = 8;
		int[] xOffset = {-1, 0, 1, 1,  1,  0, -1, -1};
		int[] yOffset = { 1, 1, 1, 0, -1, -1, -1,  0};
		
		for( int i = 0; i < state.getWidth(); ++i ) {
			for( int j = 0; j < state.getHeight(); ++j ) {
				int current = state.getSpace(i, j);
				if ( current != 0 ) {
					for( int k = 0; k < numOffset; ++k ) {
						int nextX = i + xOffset[k];
						int nextY = j + yOffset[k];
						if ( validPoint( state, nextX, nextY ) && 
							 state.getSpace( nextX, nextY ) == current ) {
							if ( current == myPlayer ) {
								meCount += 1;
							} else {
								oppoCount += 1;
							}
						}
					}
				}
			}
		}
		
		return meCount - oppoCount;
	}

	Integer winMove( BoardModel state ) {
		int value = state.winner();

		if ( value == myPlayer ) {
			return 1;
		}
		else if ( value == otherPlayer )
		{
			return -1;
		}
		return 0;
	}
	
	// This one help favor the one at the center than at the boundary
	Integer piecesNearCenter( BoardModel state ) {
		Integer meScore = 0;
		Integer oppoScore = 0;
		int otherPlayer = ( myPlayer == 1 ) ? 2 : 1;
		
		Integer centerX = state.getWidth() / 2;
		Integer centerY = state.getHeight() / 2;
		
		for( int i = 0; i < state.getWidth(); ++i ) {
			for( int j = 0; j < state.getHeight(); ++j ) {
				int value = state.getSpace( i, j );
				if ( value == myPlayer ) {
					meScore += Math.abs(i - centerX) + Math.abs(j - centerY);
				}
				else if ( value == otherPlayer )
				{
					oppoScore += Math.abs(i - centerX) + Math.abs(j - centerY);
				}
			}
		}
		
		return oppoScore - meScore;
	}
	
	/* ======================================================= */
	
	
	// Detect if this horizontal is winning path with assumption is that
	//  Direction
	//  0   1  2
	//     f1  3
	Integer getFeature( BoardModel state, Point f, int dire, int current ) {
		
		int oppoCurrent = ( current == 1 )? 2: 1;
		
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
		while( true ) {
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
									value = scoreLoseWin;
								}
							}
							
							if ( current == myPlayer ) {
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
	
	Integer staticEval( BoardModel state ) {
		Integer score = 0;
		
		Integer winMoveWeight = scoreLoseWin;
		Integer winMoveValue = winMove( state );
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
	
	// This children function deal with gravity on
	List<BoardModel> getChildrenWithGravity( BoardModel state, int current, boolean isMax ) {
		
		List<BoardModel> children = new ArrayList<BoardModel>();
		
		for( int i = 0; i < state.getWidth(); ++i ) {
			for( int j = 0; j < state.getHeight(); ++j ) {
				if ( state.getSpace( i, j ) == 0 ) {
					int newX = i;
					int newY = j - 1;
					if ( ! validPoint( state, newX, newY) ||
						 state.getSpace( newX, newY ) != 0 ) {
						byte value = 0;
						if ( current == 1) {
							value = 1;
						}
						else if ( current == 2 )
						{
							value = 2;
						}
						BoardModel newState = state.placePiece( new Point( i, j ), value );
						children.add( newState );
					}
				}
			}
		}
		
		return children;
	}
	
	List<BoardModel> getChildren( BoardModel state, int current, boolean isMax ) {
		if ( state.gravityEnabled() )
			return getChildrenWithGravity (state, current, isMax );
		List<BoardModel> children = new ArrayList<BoardModel>();
		
		int numOffset = 8;
		int[] xOffset = {0, 1, 1, 1, 0, -1, -1, -1};
		int[] yOffset = {1, 1, 0, -1, -1, -1, 0, 1};
		
		Set<String> set = new HashSet<String>();
		
			for( int i = 0; i < state.getWidth(); ++i ) {
				for( int j = 0; j < state.getHeight(); ++j ) {
					if ( state.getSpace(i, j) == 0 ) {
						// This slot has no occupied element
						
						// Check if surrounding has occupied
						for( int k = 0; k < numOffset; ++k ) {
							int nextX = i + xOffset[k];
							int nextY = j + yOffset[k];
							if ( validPoint(state, nextX, nextY) ) {
								// If the arround has at least one occupied 
								if ( state.getSpace(nextX, nextY) != 0 ) {
									
									byte value = 0;
									if ( current == 1) {
										value = 1;
									}
									else if ( current == 2 )
									{
										value = 2;
									}
									
									BoardModel newState = state.placePiece( new Point( i, j ), value );
									String stateStr = newState.toString();
									if ( ! set.contains( stateStr ) ) {
										set.add( stateStr );
										children.add( newState );
									}
								}
							}
						}
					}
				}
			}
		return children;
	}
	
	// This function helps print a list of children starting from a node
	Point getAction( BoardModel state ) {
		// This always is the max node, my player;
		// Get the current player turn
		
		// If we are out of time, stop
		if ( outOfTime() ) return new Point(0, 0);
		
		if ( state.spacesLeft == state.getHeight() * state.getWidth() ) {
			if ( state.gravityEnabled() )
				return new Point( state.getWidth() / 2, 0);
			return new Point( state.getWidth() / 2, state.getHeight() / 2);
		}
		
		int currentPlayer = currentTurn( state );
		
		// Why? I want to know who I am? Player 1 or player 2
		if ( myPlayer == -1 ) {
			myPlayer = currentPlayer;
			otherPlayer = ( myPlayer == 1 ) ? 2 : 1;
		}
		
		Point action = new Point(0, 0);

		Integer alpha = lower;
		Integer beta = upper;
		
		// true value of isMax to help order the children by their evaluations
		List<BoardModel> children = getChildren( state, currentPlayer, true );

		
		Integer v = lower;
		for( int i = 0; i < children.size(); ++i ) {
			BoardModel child = children.get(i);
			
			Integer result =  alphaBeta( child, false, alpha, beta, 2 );
			if ( result > v ) {
				v = result;
				action = child.getLastMove();
			}
			if ( v >= beta ) {
				break;
			}
			if ( v > alpha ) {
				alpha = v;
			}
		}
		
		System.out.println( action );
//		printChildren(children);
		
		return action;
	}
	
	// alpha-beta pruning
	Integer alphaBeta( BoardModel state,
				   boolean isMax,
				   Integer alpha,
				   Integer beta,
				   int depth ) {
		
		// If we are out of time, stop right away
		if ( outOfTime() ) return 0;
		
		byte winner = state.winner();

		if ( winner == 1 || winner == 2 ) {
			if ( winner == myPlayer ) {
				return scoreLoseWin;
			}
			else
			{
				return - scoreLoseWin;
			}
		}
		
		// At the depth limit
		if ( depth >= depthLimit ) {
			return staticEval( state );
		}

		// Get the current player turn
		int currentPlayer = currentTurn( state );
		
		// This is max node
		if ( isMax ) {
			List<BoardModel> children = getChildren( state, currentPlayer, true );
			Integer v = lower;
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
			Integer v = upper;
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

	// This function checks if it's out of time
	boolean outOfTime () {
		long stopTime = System.currentTimeMillis();
		if ( stopTime - startTime > myDeadLine - 600 ) {
			return true;
		}
		return false;
	}	
}
