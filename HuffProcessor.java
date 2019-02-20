import java.util.PriorityQueue;

/**
 *	Interface that all compression suites must implement. That is they must be
 *	able to compress a file and also reverse/decompress that process.
 */
public class HuffProcessor {

	public static final int BITS_PER_WORD = 8;
	public static final int BITS_PER_INT = 32;
	public static final int ALPH_SIZE = (1 << BITS_PER_WORD); // or 256
	public static final int PSEUDO_EOF = ALPH_SIZE;
	public static final int HUFF_NUMBER = 0xface8200;
	public static final int HUFF_TREE  = HUFF_NUMBER | 1;
	public static final int HUFF_COUNTS = HUFF_NUMBER | 2;

	public enum Header{TREE_HEADER, COUNT_HEADER};
	public Header myHeader = Header.TREE_HEADER;
	
	/**
	 * Compresses a file. Process must be reversible and loss-less.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be compressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	
	public void compress(BitInputStream in, BitOutputStream out) {
		int[] counts = readForCounts(in);
		HuffNode root = makeTreeFromCounts(counts);
		String[] codings = makeCodingsFromTree(root, "", new String[ALPH_SIZE + 1]);
		writeHeader(root, out);
		in.reset();
		writeCompressedBits(in, codings, out);
	}
	
	//Helper method for compress(). Returns int[] length = 256 where index = bit representation of characters in original file
	//int[val] = number of times the character occurs
	public int[] readForCounts(BitInputStream in) {
		int[] counts = new int[ALPH_SIZE];
		while (true) {
			int a = in.readBits(BITS_PER_WORD);
			if (a == -1)
				break;
			else
				counts[a]++;
		}
		return counts;
	}
	
	//Helper method for compress(). Returns root to Huffman Coding Tree 
	//Contains HuffNode for PSEUDO_EOF character with weight of 1 
	public HuffNode makeTreeFromCounts(int[] counts) {
		int track = 0; //Benchmark variable for alphabet size 
		PriorityQueue<HuffNode> pq = new PriorityQueue<>();
		for (int i = 0; i < counts.length; i++) {
			if (counts[i] != 0) {
				pq.add(new HuffNode(i, counts[i]));
				track++;
			}
		}
		System.out.println("alphabet size = " + track); //Benchmark for analysis
		pq.add(new HuffNode(PSEUDO_EOF, 1));
		while(pq.size() > 1) {
			HuffNode left = pq.remove();
			HuffNode right = pq.remove();
			HuffNode t = new HuffNode(-1, left.weight() + right.weight(), left, right);
			pq.add(t);
		}
		HuffNode root = pq.remove();
		return root;
	}
	
	//Helper method for compress(). Returns String[] length = 257 where index = bit representation of characters in original file
	//String[val] = Huffman coding String built from traversing tree 
	public String[] makeCodingsFromTree(HuffNode node, String str, String[] codings) {
		if (node.left() == null && node.right() == null) {
			codings[node.value()] = str;
		}
		if (node.left() != null) {
			makeCodingsFromTree(node.left(), str+"0", codings);
		}
		if (node.right() != null) {
			makeCodingsFromTree(node.right(), str+"1", codings);
		}
		return codings;
	}	
	
	public void writeHeader(HuffNode root, BitOutputStream BOS) {
		BOS.writeBits(BITS_PER_INT, HUFF_TREE);
		writeTree(root, BOS);
	}
	
	public void writeTree(HuffNode node, BitOutputStream out) {
		if (node.left() == null && node.right() == null) {
			out.writeBits(1, 1);
			out.writeBits(BITS_PER_WORD + 1, node.value());
			return;
		}
		else {
			out.writeBits(1, 0);
//			if (root.left() != null) {
//				writeTree(root.left(), BOS);
//			}
//			if (root.right() != null) {
//				writeTree(root.right(), BOS);
//			}
			writeTree(node.left(), out);
			writeTree(node.right(), out);
		}
	}
	
	public void writeCompressedBits(BitInputStream in, String[] codings, BitOutputStream out) {
		while (true) {
			int bits = in.readBits(BITS_PER_WORD);
			if (bits == -1) {
				out.writeBits(codings[PSEUDO_EOF].length(), Integer.parseInt(codings[PSEUDO_EOF], 2));
				break;
			}
			out.writeBits(codings[bits].length(), Integer.parseInt(codings[bits], 2));
		}
	}


	/**
	 * Decompresses a file. Output file must be identical bit-by-bit to the
	 * original.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be decompressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void decompress(BitInputStream in, BitOutputStream out){
		//check HUFF_TREE
		int check = in.readBits(BITS_PER_INT);
		if (check != HUFF_TREE) {
			throw new HuffException("wrong huff number - not valid compressed file");
		}
		
		HuffNode root = readTreeHeader(in);
		readCompressedBits(in, out, root);
	}
	
	//Helper method for decompress(), builds Huffman tree
	public HuffNode readTreeHeader(BitInputStream in) {
		int bit = in.readBits(1);
		if (bit == -1) {
			throw new HuffException("no PSEUDO_EOF");
		}
		if (bit == 1) {
			HuffNode node = new HuffNode(in.readBits(BITS_PER_WORD + 1), 0); 
			return node;
		}
		if (bit == 0) {
			HuffNode left = readTreeHeader(in);
			HuffNode right = readTreeHeader(in);
			HuffNode node = new HuffNode(0, 0, left, right);
			return node;
		}
		else 
			throw new HuffException ("bad input");
	}
	
	//Helper method for decompress(), reads compressed bits and write into output stream 
	public void readCompressedBits (BitInputStream in, BitOutputStream out, HuffNode root) {
		HuffNode current = root;
		int BIT;
		while (true) {
			BIT = in.readBits(1);
			if (BIT == -1) {
				throw new HuffException("no PSEUDO_EOF");
			}
			if (BIT == 1) {
				current = current.right();
			}
			else if (BIT == 0) {
				current = current.left();
			}
			if (current.left() == null && current.right() == null) {
				if (current.value() == PSEUDO_EOF)
					break;
				else {
					out.writeBits(BITS_PER_WORD, current.value());
					current = root;
				}
			}
		}
	}
	
	public void setHeader(Header header) {
        myHeader = header;
        System.out.println("header set to "+myHeader);
    }
}
























