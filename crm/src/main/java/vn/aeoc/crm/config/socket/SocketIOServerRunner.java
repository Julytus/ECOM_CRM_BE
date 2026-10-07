//package vn.aeoc.crm.config.socket;
//
//import com.corundumstudio.socketio.SocketIOServer;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.DisposableBean;
//import org.springframework.beans.factory.InitializingBean;
//import org.springframework.stereotype.Component;
//
//@Slf4j
//@Component
//public class SocketIOServerRunner implements InitializingBean, DisposableBean {
//
//    private final SocketIOServer server;
//
//    public SocketIOServerRunner(SocketIOServer server) {
//        this.server = server;
//    }
//
//    @Override
//    public void afterPropertiesSet() {
//        server.start();
//        log.info("Socket.IO server started on port {}", server.getConfiguration().getPort());
//    }
//
//    @Override
//    public void destroy() {
//        server.stop();
//        log.info("Socket.IO server stopped");
//    }
//}