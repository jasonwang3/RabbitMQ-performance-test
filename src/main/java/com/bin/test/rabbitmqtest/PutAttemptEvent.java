package com.bin.test.rabbitmqtest;

import com.everbridge.notification.expansion.model.Attempt;
import com.everbridge.notification.expansion.model.AttemptPath;
import com.everbridge.notification.expansion.model.support.AttemptPathType;
import org.apache.commons.cli.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by jason on 16-3-31.
 */
public class PutAttemptEvent {
    public static void main(String[] args) {
        Options options = getOptions();
        CommandLineParser parser = new GnuParser();

        try {
            CommandLine cmd = parser.parse(options, args);
            String host = strArg(cmd, 'h', "127.0.0.1");
            String username = strArg(cmd, 'n', null);
            String password = strArg(cmd, 'p', null);
            String vhost = strArg(cmd, 'v', null);
            int port = intArg(cmd, 'o', 5672);
            String exchange = strArg(cmd, 'e', null);
            String routingKey = strArg(cmd, 'r', null);
            int count = intArg(cmd, 'c', 10000);
            LinkedBlockingQueue<Attempt> linkedBlockingQueue = new LinkedBlockingQueue();

            ExecutorService fixedThreadPool = Executors.newFixedThreadPool(100);

            CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory();
            cachingConnectionFactory.setHost(host);
            cachingConnectionFactory.setVirtualHost(vhost);
            cachingConnectionFactory.setUsername(username);
            cachingConnectionFactory.setPassword(password);
            cachingConnectionFactory.setPort(port);
            cachingConnectionFactory.setChannelCacheSize(25);
//            cachingConnectionFactory.setCacheMode(CachingConnectionFactory.CacheMode.CONNECTION);
//            cachingConnectionFactory.setConnectionCacheSize(3);

            RabbitTemplate rabbitTemplate = new RabbitTemplate();
            rabbitTemplate.setConnectionFactory(cachingConnectionFactory);
            MessageConverter convert = new JsonMessageConverter();

            rabbitTemplate.setMessageConverter(convert);

            for (int i = 0; i < count; i++) {
                Attempt attempt = new Attempt();
                attempt.setAttempt(i + 1);
                attempt.setBroadcastId("56fbd14ae4b0533bca1fd624");
                attempt.setContactId("444249942261817");
                attempt.setFirstName("echo");
                attempt.setLastName("xu");
                AttemptPath attemptPath = new AttemptPath();
                attemptPath.setPathType(AttemptPathType.EMAIL);
                attemptPath.setPathValue("abcdkededdada");
                attemptPath.setCallerId("12121212");
                attemptPath.setCountryCode("US");
                attemptPath.setConfirmationPhoneNumber("112112121");
                attempt.setAttemptPath(attemptPath);
                linkedBlockingQueue.put(attempt);

            }
            long beginTime = System.currentTimeMillis();

            for (int i = 0; i < 100; i++) {
                final int index = i;
                fixedThreadPool.execute(new Runnable() {
                    public void run() {
                        boolean flag = true;
                        while (flag) {
                            Attempt attempt = linkedBlockingQueue.poll();
                            if (attempt != null) {
                                rabbitTemplate.convertAndSend(exchange, routingKey, attempt);
                            } else {
                                flag = false;
                            }
                        }

                    }
                });
            }
            fixedThreadPool.shutdown();
            while (!fixedThreadPool.isTerminated());
            long endTime = System.currentTimeMillis();
            System.out.println("total cost " + (endTime-beginTime));
        } catch (Exception e) {
            e.printStackTrace();
        }



    }


    private static Options getOptions() {
        Options options = new Options();
        options.addOption(new Option("h", "host", true, "connection host"));
        options.addOption(new Option("n", "username", true, "username"));
        options.addOption(new Option("p", "password", true, "password"));
        options.addOption(new Option("v", "vhost", true, "vhost"));
        options.addOption(new Option("o", "port", true, "port"));
        options.addOption(new Option("e", "exchange", true, "exchange name"));
        options.addOption(new Option("r", "routingKey", true, "routingKey name"));
        options.addOption(new Option("c", "count", true, "total count"));
        return options;
    }

    private static String strArg(CommandLine cmd, char opt, String def) {
        return cmd.getOptionValue(opt, def);
    }

    private static int intArg(CommandLine cmd, char opt, int def) {
        return Integer.parseInt(cmd.getOptionValue(opt, Integer.toString(def)));
    }
}
