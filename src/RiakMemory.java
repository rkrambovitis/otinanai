import com.basho.riak.client.*;

package gr.phaistosnetworks.admin.otinanai;

class RiakMemory {
   public RiakMemory(kw, al, log, rt, ps, host, metric) {
      keyWord = kw;
      recType = rt;
      logger = log;
      previewSamples = ps;
      add(host, metric);
   }

}
