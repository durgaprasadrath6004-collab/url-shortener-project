import React, { useState } from 'react'
import { useStoreContext } from '../../contextApi/ContextApi';
import { useForm } from 'react-hook-form';
import { data } from 'autoprefixer';
import TextField from '../TextField';
import { Tooltip } from '@mui/material';
import { RxCross2 } from 'react-icons/rx';
import api from '../../api/api';
import toast from 'react-hot-toast';
import { getFrontendUrl } from '../../utils/helper';
import { useQueryClient } from 'react-query';
import { useNavigate } from 'react-router-dom';

const CreateNewShorten = ({ setOpen, refetch }) => {
    const { token } = useStoreContext();
    const navigate = useNavigate();
    const [loading, setLoading] = useState(false);
    const queryClient = useQueryClient();

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm({
    defaultValues: {
      originalUrl: "",
    },
    mode: "onTouched",
  });

  const createShortUrlHandler = async (data) => {
    if (!token) {
        toast.error("Please login first to create short URLs!");
        navigate("/login");
        return;
    }
    setLoading(true);
    try {
        let originalUrl = data.originalUrl?.trim() || "";
        if (!originalUrl.startsWith("http://") && !originalUrl.startsWith("https://")) {
            originalUrl = "https://" + originalUrl;
        }

        const { data: res } = await api.post("/api/urls/shorten", { originalUrl }, {
            headers: {
              "Content-Type": "application/json",
              Accept: "application/json",
              Authorization: "Bearer " + token,
            },
          });

          const backendUrl = (import.meta.env.VITE_BACKEND_URL || "https://url-shortener-sb-g3if.onrender.com").replace(/\/$/, "");
          const shortenUrl = `${backendUrl}/${res.shortUrl}`;

          if (navigator && navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(shortenUrl).then(() => {
              toast.success("Short URL Copied to Clipboard", {
                position: "bottom-center",
                className: "mb-5",
                duration: 3000,
              });
            }).catch(() => {
              toast.success("Short URL created successfully!", {
                position: "bottom-center",
                className: "mb-5",
                duration: 3000,
              });
            });
          } else {
            toast.success("Short URL created successfully!", {
              position: "bottom-center",
              className: "mb-5",
              duration: 3000,
            });
          }

          try {
            await queryClient.invalidateQueries(["my-shortenurls"]);
            await queryClient.invalidateQueries("url-totalclick");
          } catch (e) {
            console.error("Query invalidation error:", e);
          }

          if (refetch) {
            await refetch();
          }
          reset();
          setOpen(false);
    } catch (error) {
        console.error("Create short URL error:", error);
        const errorMsg = error?.response?.data?.message || error?.response?.data?.error || (error?.response?.status === 403 ? "Session expired, please re-login" : null) || error?.message || "Failed";
        toast.error(`Create ShortURL Failed: ${errorMsg}`);
        if (error?.response?.status === 401 || error?.response?.status === 403) {
            navigate("/login");
        }
    } finally {
        setLoading(false);
    }
  };


  return (
    <div className=" flex justify-center items-center bg-white rounded-md">
    <form
        onSubmit={handleSubmit(createShortUrlHandler)}
        className="sm:w-[450px] w-[360px] relative  shadow-custom pt-8 pb-5 sm:px-8 px-4 rounded-lg"
      >

        <h1 className="font-montserrat sm:mt-0 mt-3 text-center  font-bold sm:text-2xl text-[22px] text-slate-800 ">
                Create New Shorten Url
        </h1>

        <hr className="mt-2 sm:mb-5 mb-3 text-slate-950" />

        <div>
          <TextField
            label="Enter URL"
            required
            id="originalUrl"
            placeholder="https://example.com"
            type="url"
            message="Url is required"
            register={register}
            errors={errors}
          />
        </div>

        <button
          className="bg-customRed font-semibold text-white w-32  bg-custom-gradient  py-2  transition-colors  rounded-md my-3"
          type="submit"
        >
          {loading ? "Loading..." : "Create"}
        </button>

        {!loading && (
          <Tooltip title="Close">
            <button
              disabled={loading}
              onClick={() => setOpen(false)}
              className=" absolute right-2 top-2  "
            >
              <RxCross2 className="text-slate-800   text-3xl" />
            </button>
          </Tooltip>
        )}

      </form>
    </div>
  )
}

export default CreateNewShorten