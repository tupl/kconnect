import connectK.CKPlayer;
import connectK.BoardModel;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IntelligentAI extends CKPlayer {
	
	int depthLimit = 1;
	int maximumDept = 6;
	Integer lower = Integer.MIN_VALUE;
	Integer upper = Integer.MAX_VALUE;
	Integer scoreLoseWin = 100000000;
	Point lastPoint = new Point();
	int myPlayer = -1;
	int otherPlayer = -1;
	
	// startTime
	long startTime;
	int myDeadLine;
	
	public IntelligentAI(byte player, BoardModel state) {
		super(player, state);
		teamName = "IntelligentAI";
	}
	
	// This one return the current player's turn
	byte currentTurn(BoardModel state) {
		int player1 = 0;
		int player2 = 0;
		for(int i=0; i<state.getWidth(); ++i) {
			for(int j=0; j<state.getHeight(); ++j) {
				if(state.getSpace(i, j) == 1) {
					player1 += 1;
				}
				else if (state.getSpace(i, j) == 2)
				{
					player2 += 1;
				}
			}
		}
		
		if (player1 == player2) {
			// player 1 go first
			return 1;
		}
		else
		{
			// now player 2's turn
			return 2;
		}
	}
	
	boolean validPoint( BoardModel state, Point pt ) {
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
	
	/*
	 * Input: f1, f2 are otherPlayer
	 * 		  Lastmove is myPlayer
	 * Ouput:
	 * 		Try to prevent the case xox for all direction
	 */
	boolean detectFeatureK_3( BoardModel state, Point f1, Point f2 ) {
		Point lm = state.getLastMove();
		int lastMovePlayer = state.getSpace( lm );
		
		// If we do not prevent attack, return false right away
		if ( state.getSpace(f1) != otherPlayer ) return false;
		if ( lastMovePlayer != myPlayer ) return false;
		
		/*
		 * 	0	1	2
		 * 		f1	3
		 * 	 		4
		 */
		
		boolean
		 withf1_0 = lm.getX() == f1.getX() + 1 && lm.getY() == f1.getY() - 1,
		 withf1_1 = lm.getX() == f1.getX() && lm.getY() == f1.getY() - 1,
		 withf1_2 = lm.getX() == f1.getX() - 1 && lm.getY() == f1.getY() - 1,
		 withf1_3 = lm.getX() == f1.getX() - 1 && lm.getY() == f1.getY(),
		 withf1_4 = lm.getX() == f1.getX() - 1 && lm.getY() == f1.getY() + 1;

		System.out.println("---------------");
		if ( withf1_0 ) System.out.println( "withf1_0" );
		if ( withf1_1 ) System.out.println( "withf1_1" );
		if ( withf1_2 ) System.out.println( "withf1_2" );
		if ( withf1_3 ) System.out.println( "withf1_3" );
		if ( withf1_4 ) System.out.println( "withf1_4" );
		
		boolean 
		 withf1 = withf1_0 || withf1_1 || withf1_2 || withf1_3 || withf1_4;
		
		boolean
		 withf2_0 = lm.getX() == f2.getX() - 1 && lm.getY() == f2.getY() + 1,
		 withf2_1 = lm.getX() == f2.getX() && lm.getY() == f2.getY() + 1,
		 withf2_2 = lm.getX() == f2.getX() + 1 && lm.getY() == f2.getY() + 1,
		 withf2_3 = lm.getX() == f2.getX() + 1 && lm.getY() == f2.getY(),
		 withf2_4 = lm.getX() == f2.getX() + 1 && lm.getY() == f2.getY() - 1;
		
		if ( withf2_0 ) System.out.println( "withf2_0" );
		if ( withf2_1 ) System.out.println( "withf2_1" );
		if ( withf2_2 ) System.out.println( "withf2_2" );
		if ( withf2_3 ) System.out.println( "withf2_3" );
		if ( withf2_4 ) System.out.println( "withf2_4" );
		
		boolean
		 withf2 = withf2_0 || withf2_1 || withf2_2 || withf2_3 || withf2_4;
		
		// If not one of two cases, then ignore
		if ( !withf1 && ! withf2 ) return false;
		
		int type;
		
		/*
		 * 	0	1	2
		 * 		f1	3
		 * 	 		4
		 */
		
		if ( f1.getX() > f2.getX() ) { type = 0; }
		else if ( f1.getX() == f2.getX() ){ type = 1; }
		else if ( f1.getY() < f2.getY() ) { type = 2; }
		else if ( f1.getY() == f2.getY() ) { type = 2; }
		else { type = 3; }
		
		// Then we need to calculate the two detected point depending
		// 	on its type.
		Point f0, f3;
		if ( type == 0 ) {
			f0 = new Point( (int) f1.getX() + 2, (int) f1.getY() - 2 );
			f3 = new Point( (int) f2.getX() - 2, (int) f2.getY() + 2 );
		}
		else if ( type == 1 ) {
			f0 = new Point( (int) f1.getX(), (int) f1.getY() - 2 );
			f3 = new Point( (int) f2.getX(), (int) f2.getY() + 2 );
		}
		else if ( type == 2 ) {
			f0 = new Point( (int) f1.getX() - 2, (int) f1.getY() - 2 );
			f3 = new Point( (int) f2.getX() + 2, (int) f2.getY() + 2 );
		}
		else if ( type == 3 ){
			f0 = new Point( (int) f1.getX() - 2, (int) f1.getY() );
			f3 = new Point( (int) f2.getX() + 2, (int) f2.getY() );
		}else {
			f0 = new Point( (int) f1.getX() - 2, (int) f1.getY() + 2 );
			f3 = new Point( (int) f2.getX() + 2, (int) f2.getY() - 2 );
		}
		
		System.out.println( "aaaaaaaaaaaaaaaaaaaaa" );
		System.out.println( f1 );
		System.out.println( f2 );
		System.out.println( state.getLastMove() );
		System.out.println( type );
		System.out.println( f0 );
		System.out.println( f3 );
		printMap( state );
		System.out.println( "bbbbbbbbbbbbbbbbbbbb" );
		
		if ( withf2 ) {
			// If happen with f2
			if ( withf2_0 && validPoint( state, f3 ) &&
				 state.getSpace( f3 ) == otherPlayer && type == 0) return true;
			if ( withf2_1 && validPoint( state, f3 ) &&
				 state.getSpace( f3 ) == otherPlayer && type == 1) return true;
			if ( withf2_2 && validPoint( state, f3 ) &&
				 state.getSpace( f3 ) == otherPlayer && type == 2) return true;
			if ( withf2_3 && validPoint( state, f3 ) &&
				 state.getSpace( f3 ) == otherPlayer && type == 3) return true;
			if ( withf2_4 && validPoint( state, f3 ) &&
				 state.getSpace( f3 ) == otherPlayer && type == 4) return true;
		}
		else {
			// If happen with f1
			if ( withf1_0 && validPoint( state, f0 ) &&
				 state.getSpace( f0 ) == otherPlayer && type == 0) return true;
			if ( withf1_1 && validPoint( state, f0 ) &&
				 state.getSpace( f0 ) == otherPlayer && type == 1) return true;
			if ( withf1_2 && validPoint( state, f0 ) &&
				 state.getSpace( f0 ) == otherPlayer && type == 2) return true;
			if ( withf1_3 && validPoint( state, f0 ) &&
				 state.getSpace( f0 ) == otherPlayer && type == 3) return true;
			if ( withf1_4 && validPoint( state, f0 ) &&
				 state.getSpace( f0 ) == otherPlayer && type == 4) return true;
		}
		
		return false;
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
	
	Integer line( BoardModel state ) {
		Integer meScore = 0;
		Integer oppoScore = 0;
		Integer maxConse = state.getkLength();
		Integer nearlyWin = maxConse - 2;
		Integer nearlyWinRelative = 5000;
		
		Integer halfMiddle = maxConse - 3;
		Integer halfMiddleRelative = 3000;
		
		Integer[] relative = {0, 0, 10, 30, 70, 150, 250};
		
		int numOffset = 5;
		int[] xOffset = {0, 1, 1, 1, -1};
		int[] yOffset = {1, 1, 0, -1, 1};
		boolean[] diag = {false, true, false, true, true};
		
		for( int i = 0; i < state.getWidth(); ++i ) {
			for( int j = 0; j < state.getHeight(); ++j ) {
	
				if ( state.getSpace(i, j) != 0 ) {
					
					int current = state.getSpace(i, j);

					// Seach in that direction
					int countDire = 0;
					
					for( int dire = 0; dire < numOffset; ++dire ) {
						int lenConse = 1;
						
						// search from two consecutive to maxConse
					
							for( int k = 1; k < maxConse; ++k ) {
								int nextX = k * xOffset[dire] + i;
								int nextY = k * yOffset[dire] + j;
								if ( validPoint(state, nextX, nextY) ) {
									
									// If same kind of color, add 1;
									if ( state.getSpace( nextX, nextY ) == current ) {
										lenConse += 1;
									}
									else
									{
										// if not same kind, now we break
										break;
									}
								}
								else
								{
									break;
								}
							}
						
						// Add point to player here
						if ( lenConse > 1) {
							// If this is my placer
							
							double alpha = ( diag[dire] ) ? 1.1: 1.0;
							int value = (int) Math.round(relative[lenConse] * alpha);
									
							if ( current == myPlayer ) {						
								meScore += value;
							}
							else
							{
								oppoScore += value;
							}
							
							
							if ( lenConse >= halfMiddle ) {
						
								// If they try to attack us
									
								Point firstPt = new Point( i, j );
								Point secondPt = new Point( i + ( lenConse - 1 ) * xOffset[dire],
															j + ( lenConse - 1 ) *yOffset[dire] );
								
								if ( detectFeatureK_3( state, firstPt, secondPt ) ) {
									// If that's the case
									System.out.println( "Defend" );
									System.out.println( firstPt );
									System.out.println( secondPt );
									System.out.println( state.getLastMove() );
									printMap(state);
									meScore += halfMiddleRelative;
								}
								
							}
							
							// If there is K - 2 piece on the borad;
							if ( lenConse >= nearlyWin ) {				
								
								int firstX = i + lenConse * xOffset[dire];
								int firstY = j + lenConse * yOffset[dire];
								int secondX = i - xOffset[dire];
								int secondY = j - yOffset[dire];
									
								if ( current == myPlayer ) {
									// If both side has space, then we can
									// easily win
									if ( validPoint(state, firstX, firstY) && 
										validPoint(state, secondX, secondY) ) {
										if ( state.getSpace(firstX, firstY) == 0 &&
											 state.getSpace(secondX, secondY) == 0 ) {
											meScore += 3 * nearlyWinRelative;
										}
									}									
								}
								else
								{
									// This is a thread, check if it's block by both
									// direction
//									
//									System.out.println( "Thread" );
//									printMap(state);
//									
									if ( validPoint(state, firstX, firstY) && 
											validPoint(state, secondX, secondY) ) {
										// If one of two heads is blocked
										if ( state.getSpace( firstX, firstY) == myPlayer ||
											 state.getSpace( secondX, secondY) == myPlayer ) {
											meScore += 3 * nearlyWinRelative;
										}
									}
								}

							}
						}
					}
					
				}
				
				
				
			}
		}
		
		return meScore - oppoScore;
	}
	
	// This one calculate the heuristics for win move
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
	
	Integer staticEval( BoardModel state ) {
		Integer score = 0;
		
		Integer winMoveWeight = scoreLoseWin;
		Integer winMoveValue = winMove( state );
		score +=  winMoveWeight * winMoveValue;
		
		Integer numberConnects = numberOfConnectLine( state );
		Integer numberConnectsWeight = 15;
		score += numberConnectsWeight * numberConnects;
		
		Integer lineEvalWeight = 1;
		Integer lineEvalValue = line( state );
		score += lineEvalWeight * lineEvalValue;
				
		Integer centerWeight = 1;
		Integer centerValue = piecesNearCenter( state );
		score += centerWeight * centerValue;
		
		return score;
	}
	
	List<BoardModel> getChildren( BoardModel state, int current, boolean isMax ) {
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
	void printChildren( List<BoardModel> children ) {
		for( int i = 0; i < children.size(); ++i ) {
			BoardModel child = children.get(i);
			
			System.out.println( "My score : " + staticEval( child ) );
			System.out.println( child.toString() );
			System.out.println( child.getLastMove() );
		}
	}
  
	Point getAction( BoardModel state ) {
		// This always is the max node, my player;
		// Get the current player turn
		
		// If we are out of time, stop
		if ( outOfTime() ) return new Point(0, 0);
		
		if ( state.spacesLeft == state.getHeight() * state.getWidth() ) {
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
				return child.getLastMove();
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
		
		// At the depth limit
		if ( depth >= depthLimit ) {
			return staticEval( state );
		}

		// Get the current player turn
		int currentPlayer = currentTurn( state );
		
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
	
	@Override
	public Point getMove(BoardModel state) {
		try
		{
			System.out.println( "Starting thinking");
			for( int i = 1; i < maximumDept; ++i ) {
				depthLimit = i + 1;
				Point newAction = getAction(state);
				System.out.println( "Depth limit " + Integer.toString(depthLimit) +  " with action : " +
									lastPoint.toString() );
				// If we do not stop due to out of time, we should save this result
				if (!outOfTime()) {
					System.out.println( "An action is saved." );
					lastPoint = newAction;
				}
				else
				{
					return lastPoint;
				}
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
	public Point getMove(BoardModel state, int deadline) {
		// Set the start time and deadline
		myDeadLine = deadline;
		startTime = System.currentTimeMillis();
		return getMove( state );
	}
}
