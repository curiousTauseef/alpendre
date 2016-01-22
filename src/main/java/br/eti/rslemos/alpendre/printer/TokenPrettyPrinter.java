/*******************************************************************************
 * BEGIN COPYRIGHT NOTICE
 * 
 * This file is part of program "alpendre"
 * Copyright 2016  Rodrigo Lemos
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * END COPYRIGHT NOTICE
 ******************************************************************************/
package br.eti.rslemos.alpendre.printer;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.Vocabulary;

public class TokenPrettyPrinter {
	public static final char[][] BOXMODEL = {
		//   0123
			"╔═╤╗".toCharArray(), // 0
			"║ │║".toCharArray(), // 1
			"╠═╪╣".toCharArray(), // 2
			"╚═╧╝".toCharArray(), // 3
		};
	
	public static final char[][] ASCIISAFE = {
		//   0123
			"+-++".toCharArray(), // 0
			"| ||".toCharArray(), // 1
			"+-++".toCharArray(), // 2
			"+-++".toCharArray(), // 3
		};
		
	private final char TOPLEFT_CORNER;
	private final char TOPRIGHT_CORNER;
	private final char BOTTOMLEFT_CORNER;
	private final char BOTTOMRIGHT_CORNER;

	private final char HORIZONTAL;
	private final char OUTER_VERTICAL;
	private final char INNER_VERTICAL;

	private final char TOP_FIELD_DIVISION;
	private final char MIDDLE_FIELD_DIVISION;
	private final char BOTTOM_FIELD_DIVISION;

	private final char LEFT_HEADER_DIVISION;
	private final char RIGHT_HEADER_DIVISION;

	private final PrintStream out;
	private final Vocabulary vocab;
	private final String[] channelNames;
	private final int[] channelMap;
	
	public TokenPrettyPrinter(char[][] model, PrintStream out, Vocabulary vocab, String[] channelNames, int[] channelMap) {
		this.out = out;
		this.vocab = vocab;
		this.channelNames = channelNames;
		this.channelMap = channelMap;

		TOPLEFT_CORNER        = model[0][0];
		TOPRIGHT_CORNER       = model[0][3];
		BOTTOMLEFT_CORNER     = model[3][0];
		BOTTOMRIGHT_CORNER    = model[3][3];

		HORIZONTAL            = model[0][1];
		OUTER_VERTICAL        = model[1][0];
		INNER_VERTICAL        = model[1][2];

		TOP_FIELD_DIVISION    = model[0][2];
		MIDDLE_FIELD_DIVISION = model[2][2];
		BOTTOM_FIELD_DIVISION = model[3][2];

		LEFT_HEADER_DIVISION  = model[2][0];
		RIGHT_HEADER_DIVISION = model[2][3];
	}

	public void printTokens(List<? extends Token> tokens) {
		Printer p = new Printer(computeWidths(tokens));

		String[] strings = new String[channelNames.length + 4];
		
		strings[0] = "#";
		strings[1] = "lin";
		strings[2] = "col";
		System.arraycopy(channelNames, 0, strings, 3, channelNames.length);
		
		strings[strings.length - 1] = "text";
		
		p.printBorder(TOPLEFT_CORNER, HORIZONTAL, TOP_FIELD_DIVISION, TOPRIGHT_CORNER);
		p.printStrings(strings);
		p.printBorder(LEFT_HEADER_DIVISION, HORIZONTAL, MIDDLE_FIELD_DIVISION, RIGHT_HEADER_DIVISION);

		Arrays.fill(strings, "");
		for (int i = 0; i < tokens.size(); i++) {
			Token token = tokens.get(i);
			
			strings[0] = p.rightAlign(0, Integer.toString(i+1));
			strings[1] = p.rightAlign(1, Integer.toString(token.getLine()));
			strings[2] = p.rightAlign(2, Integer.toString(token.getCharPositionInLine()));
			
			for (int j = 0; j < channelMap.length; j++)
				strings[3 + j] = channelMap[token.getChannel()] == j ? getDisplayName(token) : "";
				
			strings[strings.length - 1] = getText(token);
			
			p.printStrings(strings);
		}

		p.printBorder(BOTTOMLEFT_CORNER, HORIZONTAL, BOTTOM_FIELD_DIVISION, BOTTOMRIGHT_CORNER);
	}

	private String getDisplayName(Token token) {
		return vocab.getDisplayName(token.getType());
	}

	private String getText(Token token) {
		return vocab.getLiteralName(token.getType()) == null ? escape(token.getText()) : "";
	}
	
	private class Printer {
		private final int[] widths;
		private final String format;
		
		public Printer(int... widths) {
			this.widths = widths;
			
			StringBuilder format0 = new StringBuilder();
			format0.append("%c");
			for (int w : widths)
				format0.append("%-").append(w).append("s%c");
			format0.append("\n");
			
			this.format = format0.toString();
		}
		
		public void printBorder(char left, char horizontal, char division, char right) {
			StringBuilder border = new StringBuilder();
			
			border.append(left);
			for (int w : widths)
				border.append(repeat(horizontal, w)).append(division);
			
			border.setLength(border.length() -1);
			border.append(right);
			
			out.println(border.toString());
		}

		public void printStrings(String... strings) {
			Object[] values = new Object[strings.length*2 + 1];
			Arrays.fill(values, INNER_VERTICAL);
			values[0] = values[values.length-1] = OUTER_VERTICAL;
			for (int i = 0; i < strings.length; i++)
				values[i*2+1] = strings[i];
			
			out.printf(format, values);
		}

		public String rightAlign(int i, String string) {
			return repeat(' ', widths[i] - string.length()) + string;
		}
	}
		
	private int[] computeWidths(List<? extends Token> tokens) {
		int[] widths = new int[8];
		
		int maxLine = 100;
		int maxColumn = 100;
		
		for (int i = 0; i < channelNames.length; i++)
			widths[i+3] = channelNames[i].length();
		
		widths[widths.length - 1] = "TEXT".length();
		
		int size = tokens.size();
		
		for (int i = 0; i < size; i++) {
			Token token = tokens.get(i);
			
			int line = token.getLine();
			if (line > maxLine)
				maxLine = line;
			
			int column = token.getCharPositionInLine();
			if (column > maxColumn)
				maxColumn = column;
			
			int channel = channelMap[token.getChannel()];
			if (channel >= 0 && channel < channelMap.length) {
				String displayName = getDisplayName(token);
				int displayNameLength = displayName.length();
				
				if (displayNameLength > widths[3+channel])
					widths[3+channel] = displayNameLength;
			}
			
			String text = getText(token);
			int textLength = text.length();
			if (textLength > widths[widths.length -1])
				widths[widths.length -1] = textLength;
		}

		widths[0] = Integer.toString(size+1).length();
		widths[1] = Integer.toString(maxLine).length();
		widths[2] = Integer.toString(maxColumn).length();
		
		return widths;
	}

	static String repeat(char val, int length) {
		return new String(new char[length]).replace('\0', val);
	}

	private static String escape(String text) {
		StringBuilder result = new StringBuilder(escape0(text));
		
		for (int i = 0; i < result.length(); i++) {
			if (result.charAt(i) != ' ')
				break;
			
			result.replace(i, i+1, "⍽");
		}
		
		if (Pattern.matches("^(.)\\1\\1\\1\\1*$", result))
			return result.charAt(0) + " × " + result.length();
		
		for (int i = result.length() -1; i>=0; i--) {
			if (result.charAt(i) != ' ')
				break;
			
			result.replace(i, i+1, "⍽");
		}
		
		return result.toString();
	}
	
	private static String escape0(String text) {
		StringBuilder result = new StringBuilder(text);
		for (int i = result.length() -1; i>=0; i--) {
			switch (result.charAt(i)) {
			case '\\': result.replace(i, i+1, "\\\\"); continue;
			case '\b': result.replace(i, i+1, "\\b"); continue;
			case '\f': result.replace(i, i+1, "\\f"); continue;
			case '\n': result.replace(i, i+1, "\\n"); continue;
			case '\r': result.replace(i, i+1, "\\r"); continue;
			case '\t': result.replace(i, i+1, "\\t"); continue;
			}
		}
	
		return result.toString();
	}
}
