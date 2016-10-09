import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Scanner;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

import com.sun.glass.events.KeyEvent;

public class BigClock
{
	
	private static String SETTING_FILE_URL = "http://nateshot.homenet.org:8025/clock_settings.txt";
	private static final float VERSION_NUMBER = 2.2f;
	private static final String defaultSettings = "background-color:(45, 54, 45)\n" +
			"font-color:(155,172,134)\n" + "title:Clock%v" + VERSION_NUMBER + "\n" +
			"extended-state:6\n" + "query-interval:60\n" + "refresh-interval:500\n";
	private final String DEFAULT_FILEPATH = "clock_settings";
	
	private JFrame frame;
	private JLabel timeLabel;
	private JLabel dateLabel;
	private JPanel container;
	private JPanel main;
	
	private JMenuBar menuBar;
	private JMenu fileMenu; // File menu - will contain exit
	private JMenu editMenu; // Exit menu - will contain settings and preferences
	private JMenuItem exitItem; // To exit
	private JMenuItem bgColorItem;
	private JMenuItem fgColorItem;
	private JMenuItem preferenceItem;
	
	private Calendar calendar;
	private SimpleDateFormat timeFormatter;
	private SimpleDateFormat dateFormatter;
	private String defaultTitle = "CLOCK v" + VERSION_NUMBER;
	private int updateQueryInterval = 30;
	private long refreshInterval = 200;
	
	public BigClock() throws IOException
	{
		
		start();
	}
	
	private void start() throws IOException
	{
		
		frame = new JFrame( defaultTitle );
		frame.setSize( 460, 400 );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		
		buildMenuBar();
		
		main = new JPanel();
		
		setUIFont( new FontUIResource( new Font( "Arial", 0, 40 ) ) );
		
		timeLabel = new JLabel();
		dateLabel = new JLabel();
		container = new JPanel();
		timeLabel.setAlignmentX( Component.CENTER_ALIGNMENT );
		dateLabel.setAlignmentX( Component.CENTER_ALIGNMENT );
		
		container.setLayout( new BoxLayout( container, BoxLayout.Y_AXIS ) );
		
		container.add( timeLabel );
		container.add( dateLabel );
		
		main.setLayout( new GridBagLayout() );
		main.add( container );
		main.setBackground( Color.LIGHT_GRAY );
		container.setBackground( Color.LIGHT_GRAY );
		
		frame.add( main );
		frame.setExtendedState( 6 );
		frame.setIconImage( ImageIO
				.read( BigClock.class.getClassLoader().getResourceAsStream( "clock.png" ) ) );
		frame.setVisible( true );
		
		timeFormatter = new SimpleDateFormat( "kk:mm:ss" );
		dateFormatter = new SimpleDateFormat( "EEEE, MMM d, yyyy" );
		
		/*
		 * load the settings
		 */
		Scanner settingsScanner;
		try
		{
			/*
			 * If a settings file doesn't exist, settings from the Internet will be loaded.
			 */
			settingsScanner = new Scanner( new File( DEFAULT_FILEPATH ) );
		} catch ( Exception e ) // Do the following for all exceptions thrown
		{
			try
			{
				URL settingsURL = new URL( SETTING_FILE_URL );
				settingsScanner = new Scanner( settingsURL.openStream() );
			} catch ( MalformedURLException ex )
			{
				settingsScanner = new Scanner( defaultSettings );
			}
		}
		
		updateSettings( settingsScanner );
		int updateSettingsCounter = 0;
		/*
		 * Continue updating the time while the program is running, and repaint the label with the time in it...
		 */
		while ( true )
		{
			calendar = Calendar.getInstance();
			
			timeLabel.setText( timeFormatter.format( calendar.getTime() ) );
			dateLabel.setText( dateFormatter.format( calendar.getTime() ) );
			
			/* 
			 * Slow down the refresh rate so it doesn't consume
			 * all the machine's resources. 
			 */
			try
			{
				Thread.sleep( refreshInterval );
			} catch ( InterruptedException e )
			{
				System.out.println( "Error: " + e.getMessage() );
			}
			
			// update the settings only every interval period ( * 5 because
			// there are 5 * 200ms in a second)
			if ( updateSettingsCounter >= updateQueryInterval * (int) (1000 / refreshInterval) )
			{
				if ( updateSettings( settingsScanner ) )
					updateSettingsCounter = 0;
				else
					// Double the amount of time before checking for updated
					// settings if it was unsuccessful, that way,
					// we don't waste so many resources trying to update the
					// settings when its not even working.
					updateSettingsCounter = -updateQueryInterval * (int) (1000 / refreshInterval);
			}
			
			updateSettingsCounter++;
		}
	}
	
	private void buildMenuBar()
	{
		
		menuBar = new JMenuBar();
		
		buildFileMenu();
		buildEditMenu();
		
		menuBar.add( fileMenu );
		menuBar.add( editMenu );
		
		frame.setJMenuBar( menuBar );
	}
	
	private void buildFileMenu()
	{
		
		exitItem = new JMenuItem( "Exit" );
		exitItem.setMnemonic( KeyEvent.VK_X );
		exitItem.addActionListener( new ExitListener() );
		
		// Create a JMenu object for the file menu
		fileMenu = new JMenu( "File" );
		fileMenu.setMnemonic( KeyEvent.VK_F );
		
		fileMenu.add( exitItem );
		
		return;
	}
	
	private void buildEditMenu()
	{
		
		bgColorItem = new JMenuItem( "Background Color" );
		bgColorItem.setMnemonic( KeyEvent.VK_B );
		bgColorItem.addActionListener( new ColorListener() );
		
		fgColorItem = new JMenuItem( "Text Color" );
		fgColorItem.setMnemonic( KeyEvent.VK_T );
		fgColorItem.addActionListener( new ColorListener() );
		
		preferenceItem = new JMenuItem( "Preferences" );
		preferenceItem.setMnemonic( KeyEvent.VK_P );
		preferenceItem.addActionListener( new PreferenceListener() );
		
		editMenu = new JMenu( "Edit" );
		editMenu.setMnemonic( KeyEvent.VK_E );
		
		editMenu.add( bgColorItem );
		editMenu.add( fgColorItem );
		editMenu.add( preferenceItem );
		
		return;
		
	}
	
	private void saveSettings( String filePath )
	{
		
		try
		{
			PrintWriter fileOut = new PrintWriter( new File( filePath ) );
			
			fileOut.println( "background-color:" + "(" + main.getBackground().getRed() + "," +
					main.getBackground().getGreen() + "," + main.getBackground().getBlue() + ")" );
			fileOut.println( "font-color:" + "(" + timeLabel.getForeground().getRed() + "," +
					timeLabel.getForeground().getGreen() + "," +
					timeLabel.getForeground().getBlue() + ")" );
			fileOut.println( "title:" + frame.getTitle() ); // Need to replace
															// spaces with a %
			fileOut.println( "extended-state:" + frame.getExtendedState() );
			fileOut.println( "query-interval:" + updateQueryInterval );
			fileOut.println( "refresh-interval:" + refreshInterval );
			fileOut.close();
		} catch ( FileNotFoundException e )
		{
			System.out.println( "Unable to save settings to the file path: " + filePath + "\n" );
			e.printStackTrace();
		}
		
		return;
	}
	
	/**
	 * Update the clock settings from the url text file<br>
	 * http://nateshot.homenet.org:8025/clock_settings.txt
	 */
	private boolean updateSettings( Scanner settings )
	{
		
		// Reset the title just in case it is set to an error title.
		frame.setTitle( defaultTitle );
		
		try
		{
			
			String setting, selector[];
			
			/*
			 * Read all the settings from the file
			 */
			while ( settings.hasNextLine() )
			{
				
				/*
				 * Deblank the string for simpler use
				 */
				setting = removeWhiteSpaces( settings.nextLine() );
				
				/*
				 * if the line is empty, move to the next one.
				 */
				if ( setting.isEmpty() )
					continue;
				
				/*
				 * Ignore the line if it starts with a non-letter character (such as a // or # for a comment)
				 */
				if ( !Character.isLetter( setting.charAt( 0 ) ) )
					continue;
				
				/*
				 * Check if the setting is valid - with two parts. If not, move
				 * to the next line.
				 */
				selector = setting.split( ":" );
				if ( selector.length != 2 )
					continue;
				
				String tempString;
				Color newColor;
				/*
				 * Switch through possible settings
				 */
				switch ( selector[0].toLowerCase() )
				{
					case "background-color":
						/*
						 * Extract and set the color
						 */
						
						// gets the numbers separated by commas
						tempString = selector[1].substring( 1, selector[1].length() - 1 );
						
						newColor = verifyColor( tempString );
						
						main.setBackground( newColor );
						container.setBackground( newColor );
						continue;
					case "font-color":
						/*
						 * Extract and set the color
						 */
						// gets the numbers separated by commas
						tempString = selector[1].substring( 1, selector[1].length() - 1 );
						
						newColor = verifyColor( tempString );
						
						timeLabel.setForeground( newColor );
						dateLabel.setForeground( newColor );
						continue;
					
					case "title":
						frame.setTitle( respace( selector[1] ) );
						continue;
					
					case "extended-state":
						frame.setExtendedState( Integer.parseInt( selector[1] ) );
						continue;
					
					case "query-interval":
						updateQueryInterval = Integer.parseInt( selector[1] );
						continue;
					
					case "refresh-interval":
						refreshInterval = Long.parseLong( selector[1] );
						continue;
					
					default:
						continue;
				}
			}
			
		} catch ( NumberFormatException e )
		{
			System.out.println( "Error: " + e.getMessage() );
			frame.setTitle( "CLOCK - Parse Error" );
			return false;
		} catch ( Exception e )
		{
			System.out.println(
					"The clock has encoutered an unexpected error retrieving the settings file..." );
			System.out.println( e.getMessage() );
			e.printStackTrace();
			frame.setTitle( "CLOCK - Unexpected Error" );
			return false;
		}
		
		return true;
	}
	
	/**
	 * This method will replace all '%' symbols in a string with a single blank
	 * space.
	 * 
	 * @param deblankedString
	 *            The string to be re-spaced.
	 * @return A string void of any '%' symbols, which have been replaced by a
	 *         space.
	 */
	private static String respace( String deblankedString )
	{
		
		String str = "";
		char[] charArray = deblankedString.toCharArray();
		
		for ( int i = 0; i < charArray.length; i++ )
		{
			if ( charArray[i] == '%' )
				str += " ";
			else
				str += charArray[i];
		}
		
		return str;
	}
	
	/**
	 * This method takes a string in the format of "N,N,N" (excluding the
	 * quotes) and parses each N into a number which will be used for its
	 * corresponding RGB color. The string must be deblanked.
	 * 
	 * @param color
	 *            The string representing the RGB values
	 * @return A color object created from the RGB values passed in (unless the
	 *         numbers passed it were larger than 255 or less than 0, then that
	 *         R, G, or B value will be 255 or 0, respectively.
	 */
	private static Color verifyColor( String color )
	{
		
		int red, green, blue;
		
		color = removeWhiteSpaces( color );
		
		red = Integer.parseInt( color.split( "," )[0] );
		green = Integer.parseInt( color.split( "," )[1] );
		blue = Integer.parseInt( color.split( "," )[2] );
		
		/*
		 * Verify that the new colors will work.
		 */
		if ( red > 255 )
			red = 255;
		else if ( 0 > red )
			red = 0;
		if ( green > 255 )
			green = 255;
		else if ( 0 > green )
			green = 0;
		if ( blue > 255 )
			blue = 255;
		else if ( 0 > blue )
			blue = 0;
		
		return new Color( red, green, blue );
	}
	
	/**
	 * Sets the font of the UI FontUIResource passed in.
	 * 
	 * @param f
	 *            The FontUIResource to edit the font of.
	 */
	private static void setUIFont( FontUIResource f )
	{
		
		Enumeration<Object> keys = UIManager.getDefaults().keys();
		while ( keys.hasMoreElements() )
		{
			Object key = keys.nextElement();
			Object value = UIManager.get( key );
			if ( value instanceof FontUIResource )
			{
				FontUIResource orig = (FontUIResource) value;
				Font font = new Font( f.getFontName(), orig.getStyle(), f.getSize() );
				UIManager.put( key, new FontUIResource( font ) );
			}
		}
	}
	
	/**
	 * This method removes any blanks (new line character, space, or new line
	 * character) from a string.
	 * 
	 * @param str
	 *            The string to remove white spaces from.
	 * @return The same string passed in, void of any white spaces.
	 */
	public static String removeWhiteSpaces( String str )
	{
		
		int index, length = str.length();
		char ch;
		String deblanked = "";
		
		for ( index = 0; index < length; index++ )
		{
			ch = str.charAt( index );
			if ( !Character.isSpaceChar( ch ) )
			{
				deblanked += ch;
			}
		}
		
		return deblanked;
	}
	
	public static void main( String[] args ) throws IOException
	{
		
		new BigClock();
	}
	
	private class ExitListener implements ActionListener
	{
		
		public void actionPerformed( ActionEvent e )
		{
			
			saveSettings( DEFAULT_FILEPATH );
			System.exit( 0 );
		}
	}
	
	private class ColorListener implements ActionListener
	{
		
		public void actionPerformed( ActionEvent e )
		{
			
			// Show color picker and set the setting accordingly (BG or FG)
			Color newColor = JColorChooser.showDialog( frame, "Select a Color", Color.BLACK );
			
			if ( e.getSource() == bgColorItem )
			{
				// Handle Background Color and return
				main.setBackground( newColor );
				container.setBackground( newColor );
				
				return;
			}
			
			// Handle FG color and return
			timeLabel.setForeground( newColor );
			dateLabel.setForeground( newColor );
			return;
		}
	}
	
	public class PreferenceListener implements ActionListener
	{
		
		public void actionPerformed( ActionEvent e )
		{
			
			// Open preferences GUI
			
			saveSettings( DEFAULT_FILEPATH );
			return;
		}
	}
	
}
