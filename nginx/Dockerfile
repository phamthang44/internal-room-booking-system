FROM nginx:stable-alpine

# Copy your custom configuration into the image
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
