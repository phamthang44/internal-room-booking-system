FROM nginx:stable-alpine

# Copy your custom global configuration into the image
COPY nginx/nginx.conf /etc/nginx/nginx.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
