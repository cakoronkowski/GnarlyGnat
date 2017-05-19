package services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import data.ChunkAudit;
import data.ChunkStatus;
import data.GetChunkResponse;
import data.TorrentCache;
import db.DbManager;
import helpers.CryptoHelper;
import lombok.NonNull;
import org.asynchttpclient.Response;

import java.io.IOException;

/**
 * Created by cakor on 11/30/2016.
 */
public class ChunkResposeHandler {


    public static void handle(Response res)
    {
        long endEpoch = System.currentTimeMillis();
        long startEpoch = Long.parseLong(res.getHeader("start_chunk_req"));
        int status = res.getStatusCode();
        ChunkAudit chunkAudit = new ChunkAudit();
        chunkAudit.CreateEpoch = System.currentTimeMillis();
        chunkAudit.StartReqTime = startEpoch;
        chunkAudit.EndReqTime = endEpoch;
        chunkAudit.ChunkFailed = true;

        if(status==200)
        {
            ObjectMapper mapper = new ObjectMapper().registerModule(new ParameterNamesModule())
                    .registerModule(new Jdk8Module())
                    .registerModule(new JavaTimeModule());
            String body  = res.getResponseBody();
            try {


                GetChunkResponse payload = mapper.readValue(body, GetChunkResponse.class);
                if(CryptoHelper.check(payload.getData(),TorrentCache.getTorrentById(payload.getTorrentId()).getHashList()[payload.getChunkNumber()])) {
                    TorrentCache.getTorrentById(payload.getTorrentId()).getFileManager().writeChunk(payload.getData(), payload.getChunkNumber());
                    TorrentCache.getTorrentById(payload.getTorrentId()).setChunkStatus(payload.getChunkNumber(), ChunkStatus.COMPLETE);
                    DbManager.getInstance().UpdateClientTorrent( TorrentCache.getTorrentById(payload.getTorrentId()));
                    chunkAudit.TorrentId = payload.getTorrentId();
                    chunkAudit.ChunkNum = payload.getChunkNumber();
                    chunkAudit.ChunkFailed = false;
                }else
                {
                    TorrentCache.getTorrentById(payload.getTorrentId()).addIncompleteChunk(payload.getChunkNumber());
                }
            } catch (IOException e) {
                System.err.println("BODY:" + body);
                e.printStackTrace();
            }
        }
        else if(status==404)
        {
            ObjectMapper mapper = new ObjectMapper();
            try {
                GetChunkResponse payload = mapper.readValue(res.getResponseBody(), GetChunkResponse.class);
                TorrentCache.getTorrentById(payload.getTorrentId()).addIncompleteChunk(payload.getChunkNumber());
                chunkAudit.TorrentId = payload.getTorrentId();
                chunkAudit.ChunkNum = payload.getChunkNumber();

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        else if(status == 500)
        {
            if (res.hasResponseHeaders())
            {
                chunkAudit.TorrentId = res.getHeader("torrentId");
                chunkAudit.ChunkNum = Integer.parseInt(res.getHeader("chunk"));
                updateFailedChunk(res.getHeader("torrentId"), Integer.parseInt(res.getHeader("chunk")));
            }
        }
        else if(status == 422)
        {
            System.out.println("You done goofed with the entities, seeding client cannot parse chunk request");
        }
        else
        {
            //it is my professional opinion, that now is the time to PANIC!!!
            System.out.println("it is my professional opinion, that now is the time to PANIC!!! ChunkResponseHandler status: " + status);
        }

        if(chunkAudit.TorrentId != null && !chunkAudit.TorrentId.isEmpty())
        {
            TorrentCache.getTorrentById(chunkAudit.TorrentId).Audit.ChunkAudits.add(chunkAudit);
        }
    }

    public static void updateFailedChunk(@NonNull String id, int chunkNum)
    {
        TorrentCache.getTorrentById(id).addIncompleteChunk(chunkNum);
    }
}
