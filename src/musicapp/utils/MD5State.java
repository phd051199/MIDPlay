package musicapp.utils;

class MD5State {

    int[] state;
    long count;
    byte[] buffer;

    public MD5State() {
        this.buffer = new byte[64];
        this.count = 0L;
        this.state = new int[4];
        this.state[0] = 1732584193;
        this.state[1] = -271733879;
        this.state[2] = -1732584194;
        this.state[3] = 271733878;
    }

    public MD5State(MD5State from) {
        this();

        int i;
        for (i = 0; i < this.buffer.length; ++i) {
            this.buffer[i] = from.buffer[i];
        }

        for (i = 0; i < this.state.length; ++i) {
            this.state[i] = from.state[i];
        }

        this.count = from.count;
    }
}
