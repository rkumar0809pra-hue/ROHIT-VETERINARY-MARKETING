FROM node:24-bookworm-slim
WORKDIR /app
COPY --chown=node:node . .
RUN mkdir -p /app/data && chown node:node /app/data
USER node
ENV HOST=0.0.0.0 PORT=3000 DATA_FILE=/app/data/marketing.sqlite
EXPOSE 3000
CMD ["node", "server.mjs"]
